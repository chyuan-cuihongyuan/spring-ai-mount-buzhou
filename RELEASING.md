# 发布指南（Releasing）

本文档说明如何将 Buzhou（`io.github.chyuan-cuihongyuan:buzhou-*`）发布到 Maven Central（Central Portal 通道）。对应 spec [09-modules-engineering.md](docs/spec/09-modules-engineering.md) 的发布工程化（ticket 20）。

## 1. 一次性前置（人工）

首次发布前，由维护者各自完成下列五项。GitHub Actions 仓库 Secrets 与 Portal 命名空间注册**无法由代码代办**。

| # | 事项 | 说明 |
|---|------|------|
| 1 | **Central Portal 命名空间** | 在 [central.sonatype.com](https://central.sonatype.com) 注册并验证命名空间 `io.github.chyuan-cuihongyuan`（GitHub 仓库的 GitHub Pages / 仓库验证）。 |
| 2 | **GPG 签名密钥** | 生成发布专用 GPG key（RSA ≥ 3072）；公钥发布到 [keys.openpgp.org](https://keys.openpgp.org) 与 [keyserver.ubuntu.com](https://keyserver.ubuntu.com)，否则 Central 校验签名会失败。 |
| 3 | **仓库 Secrets** | 在仓库 Settings → Secrets 录入四项（见下表）。 |
| 4 | **LICENSE 文件** | 仓库根补 Apache-2.0 全文（POM 已声明许可证；仓库级 LICENSE 文件仍需人工落盘）。 |
| 5 | **Portal 用户令牌** | Central Portal → Account → Generate User Token；`CENTRAL_USERNAME` / `CENTRAL_TOKEN` 用令牌值，**不是**门户登录密码。 |
| 6 | **阿里云镜像已在 settings.xml**（已处理） | 阿里云镜像（绕本机 TLS 指纹拦截）已从根 POM 迁至仓库根 `settings.xml`（本机 `cp settings.xml ~/.m2/settings.xml`）；POM 不含 `<repositories>`，已发布 POM 干净、不传染下游、Central 不拒收。CI 直连 central，无需镜像。 |

### 仓库 Secrets（Settings → Secrets and variables → Actions）

| Secret 名 | 内容 |
|-----------|------|
| `GPG_PRIVATE_KEY` | 私钥的 **base64** 编码串：`gpg --armor --export-secret-keys <KEYID> \| base64` |
| `GPG_PASSPHRASE` | 上述私钥的 passphrase |
| `CENTRAL_USERNAME` | Central Portal 用户令牌用户名 |
| `CENTRAL_TOKEN` | Central Portal 用户令牌密码 |

> GPG 私钥轮换 / 多人维护的 key 归属（个人 key vs 项目 key）见 spec 09 开放问题 #7，首次发布前需在此补充约定。

## 2. 版本号策略

- 全模块同版本演进，统一由 [buzhou-bom](buzhou-bom/pom.xml) 收口为 `${project.version}`；父 POM 是单一版本源，子模块**不**声明自己的 `<version>`。
- 父 POM `requireSameVersions` enforcer 规则在 `validate` 阶段（含 CI `verify`）守门：禁止同一 POM 内对 `io.github.chyuan-cuihongyuan` 的依赖声明不同版本。
- **首发版本 `0.1.0`**：API 稳定前遵循 `0.x` 语义（minor 版本可破兼容）；待公共 API（`api` 子包）稳定后再发布 `1.0.0`。
- 与 Spring AI 2.0.x 的兼容矩阵在 [README.md](README.md) 维护。
- 二进制兼容检查（japicmp/revapi）与 `internal` 包豁免为开放问题（spec 09 #2），暂不阻塞发布。

## 3. 本地 dry-run（出齐签名构件）

`release` profile 会在 `verify` 阶段产出 `-sources.jar`、`-javadoc.jar` 与 `.asc` 签名；用于本地核对构件完整性，**不**触发上传。

```bash
# 需本地有 GPG key 且 GPG_PASSPHRASE 已导出为环境变量
export GPG_PASSPHRASE='<your-passphrase>'
mvn -Prelease verify
```

核对产物（以 buzhou-core 为例）：

```bash
ls buzhou-core/target/*.asc
# buzhou-core-0.1.0-SNAPSHOT.jar.asc          buzhou-core-0.1.0-SNAPSHOT-sources.jar.asc   buzhou-core-0.1.0-SNAPSHOT-javadoc.jar.asc   buzhou-core-0.1.0-SNAPSHOT.pom.asc
```

> 本机无 GPG key 时，仍可用 `mvn -Prelease verify -Dgpg.skip=true` 验证 sources/javadoc 附加是否就位；签名正确性须在具备 key 的环境（含 CI）核对。

## 4. 正式发布

1. **定版**：将 SNAPSHOT 改为发布版（全模块同版本）。
   ```bash
   mvn -B versions:set -DnewVersion=0.1.0 -DprocessAllModules
   mvn -B versions:commit
   ```
   确认 `git diff` 仅改版本号后提交。
2. **打 tag 并推送**（触发 [release.yml](.github/workflows/release.yml)）：
   ```bash
   git tag v0.1.0
   git push origin v0.1.0
   ```
3. **CI 上传至 staging**：workflow 运行 `mvn -Prelease deploy` → GPG 签名 → `central-publishing-maven-plugin` 上传至 Central Portal staging（`autoPublish=false`，上传即停）。
4. **人工发布**：在 Central Portal 控制台核对 staging 构件（签名、sources、javadoc、POM 元数据齐全）后手动 Publish，同步至 Maven Central。
5. **回归 SNAPSHOT**：发布后将版本号推进回 SNAPSHOT（如 `0.1.1-SNAPSHOT`）并提交。
6. **examples 不发布**：[examples/pom.xml](examples/pom.xml) 已设 `maven.deploy.skip=true`，聚合示例与评测脚本不计入发布构件。

## 5. 排障速查

- **签名失败（no passphrase / Inappropriate ioctl）**：确认 `GPG_PASSPHRASE` 环境变量已传入；maven-gpg-plugin 3.x 已带 `--pinentry-mode loopback`，旧版需在 `gpg` 配置补 `allow-loopback-pinentry`。
- **Portal 401 / Unauthorized**：`CENTRAL_USERNAME` / `CENTRAL_TOKEN` 必须用 **User Token**，不是门户登录密码；确认命名空间已验证。
- **签名校验失败（key not found）**：公钥未发布到 Portal 信任的 keyserver，回第 1 节第 2 项。
- **POM 元数据校验失败**：Central 硬性要求 name/description/url/license/scm/developers/issueManagement，已在父 POM 声明并被全模块继承。
- **已发布 POM 干净（无 repositories）**：阿里云镜像在仓库根 `settings.xml`（不进 POM），已发布 POM 不含 `<repositories>`，Central 不会因镜像拒收。


## SBOM 附着（T128 / impl-103）

release workflow 在 deploy 后自动跑 `mvn -Psupply-chain package` 生成 CycloneDX 聚合 BOM
（json+xml），并经 softprops/action-gh-release 附着到 GitHub Release——发布物供应链可审计面。
人工核对：Release 页面附件含 `bom.json` / `bom.xml`。
