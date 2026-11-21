"""Prepare and validate release metadata locally, without making network requests."""

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import zipfile


def read_properties(content):
    properties = {}
    for line in content.splitlines():
        if line.strip() and not line.lstrip().startswith(("#", "!")) and "=" in line:
            key, value = line.split("=", 1)
            properties[key.strip()] = value.strip()
    return properties


def detect_version_change(root, event):
    """Compare push endpoints, including pushes containing multiple commits."""
    if event.get("ref") != "refs/heads/main" or event.get("deleted"):
        return {"publish": "false"}
    before, after = event["before"], event["after"]
    if before == "0" * 40:
        # Creating a branch has no previous version to compare.
        return {"publish": "false"}
    versions = []
    for revision in (before, after):
        if not re.fullmatch(r"[0-9a-f]{40}", revision):
            raise ValueError("Push endpoints must be full Git commit hashes")
        content = subprocess.check_output(
            ["git", "show", f"{revision}:gradle.properties"], cwd=root, encoding="utf-8"
        )
        version = read_properties(content)["mod_version"]
        if not version:
            raise ValueError("mod_version must not be empty")
        versions.append(version)
    previous, current = versions
    return {"publish": str(previous != current).lower(),
            "previous_version": previous, "version": current}


def prepare(root, tag=None, release_type="release"):
    properties = read_properties((root / "gradle.properties").read_text(encoding="utf-8"))

    version = properties["mod_version"]
    mod_id = properties["mod_id"]
    if not re.fullmatch(r"\d+(?:\.\d+)+(?:-[0-9A-Za-z.-]+)?(?:\+[0-9A-Za-z.-]+)?", version):
        raise ValueError("mod_version must be a version such as 1.1 or 1.2-beta.1")
    if not re.fullmatch(r"[a-z][a-z0-9_]{1,63}", mod_id):
        raise ValueError("Invalid mod_id")
    expected_tag = "v" + version
    if tag is not None and tag != expected_tag:
        raise ValueError(f"Release tag {tag!r} must match mod_version: {expected_tag}")
    if release_type not in ("release", "beta", "alpha"):
        raise ValueError("Release type must be release, beta, or alpha")

    changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
    headings = list(re.finditer(r"^#\s+(\S+)\s*$", changelog, re.MULTILINE))
    if not headings or headings[0].group(1) != version:
        raise ValueError(f"The first CHANGELOG.md heading must be '# {version}'")
    end = headings[1].start() if len(headings) > 1 else len(changelog)
    if not changelog[headings[0].end():end].strip():
        raise ValueError("The current changelog section is empty")

    output = root / "build/release/CHANGELOG.md"
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(changelog[headings[0].start():end].strip() + "\n", encoding="utf-8")
    return {
        "version": version,
        "tag": expected_tag,
        "mod_id": mod_id,
        "minecraft_version": properties["minecraft_version"],
        "release_type": release_type,
        "display_name": f"{properties['mod_name']} {version} (NeoForge {properties['minecraft_version']})",
        "artifact": f"build/libs/{mod_id}-{version}.jar",
        "changelog": "build/release/CHANGELOG.md",
    }


def verify_tag(root, tag):
    head = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=root, text=True).strip()
    target = subprocess.check_output(
        ["git", "rev-parse", "--verify", f"refs/tags/{tag}^{{commit}}"], cwd=root, text=True
    ).strip()
    if target != head:
        raise ValueError(f"Checked-out commit does not match tag {tag}")


def verify_generated(root):
    changes = subprocess.check_output(
        ["git", "status", "--porcelain", "--untracked-files=all", "--", "src/generated/resources"],
        cwd=root, text=True,
    ).strip()
    if changes:
        raise ValueError("Datagen changed resources. Regenerate and commit them first:\n" + changes)


def verify_jar(root, metadata):
    artifact = root / metadata["artifact"]
    if not artifact.is_file():
        raise ValueError(f"Expected release JAR does not exist: {metadata['artifact']}")
    mod_id = metadata["mod_id"]
    with zipfile.ZipFile(artifact) as jar:
        names = set(jar.namelist())
        if any(name.startswith(".cache/") or "/verification/" in name for name in names):
            raise ValueError("Release JAR contains datagen cache or verification classes")
        toml = jar.read("META-INF/neoforge.mods.toml").decode("utf-8")
        blocks = re.split(r"^\[\[mods\]\]\s*$", toml, flags=re.MULTILINE)[1:]
        matching = [block for block in blocks if re.search(
            rf'^modId\s*=\s*"{re.escape(mod_id)}"\s*$', block, re.MULTILINE)]
        if len(matching) != 1 or not re.search(
            rf'^version\s*=\s*"{re.escape(metadata["version"])}"\s*$', matching[0], re.MULTILINE
        ):
            raise ValueError("JAR mod ID/version does not match gradle.properties")
        required = {
            f"assets/{mod_id}/models/item/random_enchanted_book_base.json",
            *[f"assets/{mod_id}/models/item/random_enchanted_book_{rarity}.json"
              for rarity in ("common", "uncommon", "rare", "epic")],
            *[f"data/{mod_id}/{mod_id}/enchantment_pool/{rarity}.json"
              for rarity in ("common", "uncommon", "rare", "epic")],
        }
        if required - names:
            raise ValueError("Release JAR is missing generated book resources: " + ", ".join(sorted(required - names)))
        for name in required:
            json.loads(jar.read(name))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--verify-tag", action="store_true")
    parser.add_argument("--check-generated", action="store_true")
    parser.add_argument("--check-jar", action="store_true")
    parser.add_argument("--detect-version-change", action="store_true")
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[2]
    tag = os.environ.get("RELEASE_TAG") or None
    try:
        if args.detect_version_change:
            event = json.loads(Path(os.environ["GITHUB_EVENT_PATH"]).read_text(encoding="utf-8"))
            metadata = detect_version_change(root, event)
        else:
            metadata = prepare(root, tag, os.environ.get("RELEASE_TYPE", "release"))
        if args.verify_tag:
            if tag is None:
                raise ValueError("RELEASE_TAG is required when verifying a release")
            verify_tag(root, tag)
        if args.check_generated:
            verify_generated(root)
        if args.check_jar:
            verify_jar(root, metadata)
        if os.environ.get("GITHUB_OUTPUT"):
            with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as output:
                for key, value in metadata.items():
                    output.write(f"{key}={value}\n")
        print(json.dumps(metadata, ensure_ascii=False, indent=2))
    except (ValueError, KeyError, OSError, subprocess.CalledProcessError, zipfile.BadZipFile) as error:
        print(f"Release validation failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
