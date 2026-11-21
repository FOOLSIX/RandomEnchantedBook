import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
import zipfile


spec = importlib.util.spec_from_file_location("release_metadata", Path(__file__).parents[1] / "release_metadata.py")
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseMetadataTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / "gradle.properties").write_text(
            "mod_id=random_enchanted_book\nmod_name=Random Enchanted Book\n"
            "mod_version=1.1\nminecraft_version=1.21.1\n", encoding="utf-8")
        (self.root / "CHANGELOG.md").write_text(
            "# 1.1\n\n## English\n\n- Custom books.\n\n## 中文\n\n- 自定义书。\n"
            "\n# 1.0\n\n- Previous release.\n", encoding="utf-8")

    def test_extracts_only_current_bilingual_changelog(self):
        metadata = release.prepare(self.root, "v1.1")
        changelog = (self.root / metadata["changelog"]).read_text(encoding="utf-8")
        self.assertIn("## English", changelog)
        self.assertIn("自定义书", changelog)
        self.assertNotIn("Previous release", changelog)
        self.assertEqual(metadata["artifact"], "build/libs/random_enchanted_book-1.1.jar")

    def test_rejects_tag_version_mismatch(self):
        for tag in ("v1.0", "1.1", "main", "v1.1\ninvalid"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                release.prepare(self.root, tag)

    def test_rejects_changelog_version_mismatch(self):
        (self.root / "CHANGELOG.md").write_text("# 1.2\n\nNewer changes.\n", encoding="utf-8")
        with self.assertRaises(ValueError):
            release.prepare(self.root)

    def test_rejects_empty_changelog(self):
        (self.root / "CHANGELOG.md").write_text("# 1.1\n\n# 1.0\n\nOld.\n", encoding="utf-8")
        with self.assertRaises(ValueError):
            release.prepare(self.root)

    def test_rejects_invalid_release_type(self):
        with self.assertRaises(ValueError):
            release.prepare(self.root, release_type="stable")

    def write_jar(self, version="1.1", omit_pool=False, extra=None):
        metadata = release.prepare(self.root)
        path = self.root / metadata["artifact"]
        path.parent.mkdir(parents=True, exist_ok=True)
        with zipfile.ZipFile(path, "w") as jar:
            jar.writestr("META-INF/neoforge.mods.toml",
                         f'[[mods]]\nmodId = "random_enchanted_book"\nversion = "{version}"\n')
            for rarity in ("common", "uncommon", "rare", "epic"):
                if not omit_pool or rarity != "epic":
                    jar.writestr(f"data/random_enchanted_book/random_enchanted_book/enchantment_pool/{rarity}.json", "{}")
                jar.writestr(f"assets/random_enchanted_book/models/item/random_enchanted_book_{rarity}.json", "{}")
            jar.writestr("assets/random_enchanted_book/models/item/random_enchanted_book_base.json", "{}")
            if extra:
                jar.writestr(extra, "test")
        return metadata

    def test_verifies_jar_metadata_and_generated_resources(self):
        release.verify_jar(self.root, self.write_jar())

    def test_rejects_wrong_version_inside_jar(self):
        metadata = self.write_jar(version="1.0")
        with self.assertRaises(ValueError):
            release.verify_jar(self.root, metadata)

    def test_rejects_missing_generated_pool(self):
        metadata = self.write_jar(omit_pool=True)
        with self.assertRaises(ValueError):
            release.verify_jar(self.root, metadata)

    def test_rejects_cache_or_verification_classes(self):
        for extra in (".cache/hash", "com/foolsix/random_enchanted_book/verification/Test.class"):
            with self.subTest(extra=extra):
                metadata = self.write_jar(extra=extra)
                with self.assertRaises(ValueError):
                    release.verify_jar(self.root, metadata)

    def test_rejects_missing_exact_artifact(self):
        metadata = release.prepare(self.root)
        with self.assertRaises(ValueError):
            release.verify_jar(self.root, metadata)

    def git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.root, text=True, stderr=subprocess.STDOUT).strip()

    def test_tag_must_point_to_checked_out_commit(self):
        self.git("init")
        self.git("config", "user.name", "Release Test")
        self.git("config", "user.email", "release-test@example.invalid")
        self.git("add", "gradle.properties", "CHANGELOG.md")
        self.git("commit", "-m", "Test release")
        self.git("tag", "v1.1")
        release.verify_tag(self.root, "v1.1")
        self.git("commit", "--allow-empty", "-m", "Later commit")
        with self.assertRaises(ValueError):
            release.verify_tag(self.root, "v1.1")

    def test_detects_untracked_generated_resources(self):
        self.git("init")
        release.verify_generated(self.root)
        resource = self.root / "src/generated/resources/new.json"
        resource.parent.mkdir(parents=True)
        resource.write_text(json.dumps({"new": True}), encoding="utf-8")
        with self.assertRaises(ValueError):
            release.verify_generated(self.root)


class VersionChangeTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init")
        self.git("config", "user.name", "Release Test")
        self.git("config", "user.email", "release-test@example.invalid")
        self.before = self.commit("mod_version=1.1\nminecraft_version=1.21.1\n")

    def git(self, *args):
        return subprocess.check_output(
            ["git", *args], cwd=self.root, text=True, stderr=subprocess.STDOUT
        ).strip()

    def commit(self, content):
        (self.root / "gradle.properties").write_text(content, encoding="utf-8")
        self.git("add", "gradle.properties")
        self.git("commit", "--allow-empty", "-m", "Test version change")
        return self.git("rev-parse", "HEAD")

    def event(self, after, **overrides):
        return {"ref": "refs/heads/main", "before": self.before, "after": after, **overrides}

    def test_version_bump_triggers_publish(self):
        after = self.commit("mod_version=1.2\n")
        result = release.detect_version_change(self.root, self.event(after))
        self.assertEqual(result, {"publish": "true", "previous_version": "1.1", "version": "1.2"})

    def test_other_properties_and_formatting_do_not_trigger_publish(self):
        after = self.commit("# mod_version=9.9\n  mod_version = 1.1  \r\nminecraft_version=1.21.2\n")
        self.assertEqual(release.detect_version_change(self.root, self.event(after))["publish"], "false")

    def test_compares_entire_push_instead_of_last_commit(self):
        self.commit("mod_version=1.2\n")
        after = self.commit("mod_version=1.2\nminecraft_version=1.21.1\n")
        self.assertEqual(release.detect_version_change(self.root, self.event(after))["publish"], "true")

    def test_bump_reverted_in_same_push_does_not_publish(self):
        self.commit("mod_version=1.2\n")
        after = self.commit("mod_version=1.1\n")
        self.assertEqual(release.detect_version_change(self.root, self.event(after))["publish"], "false")

    def test_other_branches_and_branch_deletion_do_not_publish(self):
        for overrides in ({"ref": "refs/heads/dev"}, {"ref": "refs/tags/v1.2"}, {"deleted": True}):
            with self.subTest(overrides=overrides):
                self.assertEqual(release.detect_version_change(
                    self.root, self.event(self.before, **overrides)), {"publish": "false"})

    def test_new_branch_has_no_version_change(self):
        self.assertEqual(release.detect_version_change(
            self.root, self.event(self.before, before="0" * 40)), {"publish": "false"})

    def test_missing_version_fails_before_publishing(self):
        after = self.commit("minecraft_version=1.21.1\n")
        with self.assertRaises(KeyError):
            release.detect_version_change(self.root, self.event(after))

    def test_empty_version_fails_before_publishing(self):
        after = self.commit("mod_version=\n")
        with self.assertRaises(ValueError):
            release.detect_version_change(self.root, self.event(after))

    def test_invalid_commit_hash_is_rejected(self):
        with self.assertRaises(ValueError):
            release.detect_version_change(self.root, self.event(self.before, before="main"))


if __name__ == "__main__":
    unittest.main()
