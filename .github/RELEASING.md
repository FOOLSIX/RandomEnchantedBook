# 发布到 CurseForge

首次设置：在 [CurseForge 作者后台](https://authors.curseforge.com/)创建 API Token，然后到 [GitHub Actions Secrets](https://github.com/FOOLSIX/Random-Enchanted-Book/settings/secrets/actions) 点击 **New repository secret**，名称填 `CURSEFORGE_TOKEN`，值填 Token。

每次发布：

1. 修改 `gradle.properties` 的 `mod_version`，并同步更新 `CHANGELOG.md` 顶部的版本号和更新内容。
2. 提交并推送到 `main`，**Build and Publish to CurseForge** 会先检查版本号，只有变化时才构建一次、校验并上传，发布类型为 `release`。等待工作流成功及 CurseForge 审核通过即可。

未修改 `gradle.properties` 的推送不触发工作流；修改了该文件但 `mod_version` 未变时，只运行轻量版本检查，跳过 Java、Gradle、构建及发布。PR 不自动触发。无需创建标签或 GitHub Release。

手动试运行或补发：进入 **Actions → Build and Publish to CurseForge → Run workflow**，填写要构建的 `ref`（分支、标签或提交），选择发布类型；勾选 `publish` 才会上传，否则仅构建校验。手动运行跳过版本变化判断，补发前确认该版本尚未上传。
