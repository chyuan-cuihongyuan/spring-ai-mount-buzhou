# 开源工程化

Type: task
Status: resolved
Blocked by: —

## Question

开源发布的前置事务：确定 groupId（io.github.\<GitHub用户名\>，需提供用户名并在 Sonatype Central Portal 注册命名空间）；创建 GitHub 仓库 `spring-ai-mount-buzhou`（Apache-2.0、README、CONTRIBUTING、CODE_OF_CONDUCT、issue/PR 模板）；CI（GitHub Actions：构建 + 测试 + 静态检查）；Maven Central 发布通道（central portal publisher 还是 legacy OSSRH；签名 GPG key 准备）。其中注册账号/创建仓库/生成 key 是需要人做的部分——输出一份精确 checklist；能自动做的（模板文件草稿）直接产出。

## Answer

**定案：groupId `io.github.chyuan-cuihongyuan` + Central Portal 通道；模板文件已产出；人工 checklist 如下。**

1. **groupId**：`io.github.chyuan-cuihongyuan`（仓库已存在：https://github.com/chyuan-cuihongyuan/spring-ai-mount-buzhou）。
2. **发布通道**：Central Portal（central.sonatype.com）+ `central-publishing-maven-plugin`，不走 legacy OSSRH。

### 已自动产出（本仓库内）

- `README.md`、`CONTRIBUTING.md`、`CODE_OF_CONDUCT.md`
- `.github/workflows/ci.yml`（JDK 21 + mvn verify）
- `.github/ISSUE_TEMPLATE/bug_report.md`、`feature_request.md`、`.github/PULL_REQUEST_TEMPLATE.md`

### 人工 checklist（按序）

1. 在 [Central Portal](https://central.sonatype.com) 注册账号，登记命名空间 `io.github.chyuan-cuihongyuan`（用 GitHub 账号验证）。
2. 生成 GPG key（`gpg --full-generate-key`，RSA 4096，永不过期或长有效期），`gpg --keyserver keys.openpgp.org --send-keys <KEYID>` 发布公钥；私钥 passphrase 入密码管理器。
3. GitHub 仓库 Settings → Secrets 配置：`GPG_PRIVATE_KEY`、`GPG_PASSPHRASE`、`CENTRAL_USERNAME`、`CENTRAL_TOKEN`（Central Portal 生成的 user token）。
4. 仓库添加 `LICENSE` 文件（Apache-2.0 全文，GitHub「Add license」模板一键生成）。
5. 首次发布前在父 POM 配好 `central-publishing-maven-plugin` + `maven-gpg-plugin`（实现期任务，本图不管）。
