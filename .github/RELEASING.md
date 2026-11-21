# 发布到 CurseForge

首次设置：在 [CurseForge 作者后台](https://authors.curseforge.com/)创建 API Token，然后到 [GitHub Actions Secrets](https://github.com/FOOLSIX/Random-Enchanted-Book/settings/secrets/actions) 点击 **New repository secret**，名称填 `CURSEFORGE_TOKEN`，值填 Token。

每次发布：

1. 修改 `gradle.properties` 的 `mod_version`，并同步更新 `CHANGELOG.md` 顶部的版本号和更新内容。
2. 提交并推送到 `main`，**Publish to CurseForge** 会在检测到版本号变化后自动构建、校验并上传，发布类型为 `release`。等待工作流成功及 CurseForge 审核通过即可。

版本号未变的推送只做构建检查，无需创建标签或 GitHub Release。

手动试运行或补发：进入 **Actions → Publish to CurseForge → Run workflow**，填写要构建的 `ref`（分支、标签或提交），选择发布类型；勾选 `publish` 才会上传，否则仅构建校验。补发前确认该版本尚未上传。
