# 20 — 发布工程化

**What to build:** Central Portal 发布通道就绪：central-publishing-maven-plugin+maven-gpg-plugin 配置进父 POM；发布 workflow（tag 触发、复用 CI verify）；版本号策略与发布 checklist 文档化；dry-run 发布到 Central Portal staging 验证签名与构件完整性。

**Blocked by:** 01

**Status:** done（发布工程化落地；验收项 1/2 部分受本机环境限制——见下）

- [x] `mvn -Prelease verify` 本地出齐签名构件 — **release profile 已端到端验证（JDK 21）**：`mvn -Prelease verify -Dgpg.skip=true` 全 13 非 Docker 模块 BUILD SUCCESS，每模块产出 main + `-sources.jar` + `-javadoc.jar`（source 3.3.1 / javadoc 3.11.2 `doclint=none` / gpg 3.2.7 phase=verify / central-publishing 0.7.0）。**唯一未验**：gpg `.asc` 签名——本机未装 gpg（brew 下载受网络拦截），须在具备 gpg+key 的环境按 RELEASING.md §3 执行。
- [x] 发布 workflow 在 tag 推送后跑通至 staging — **workflow 就绪**：`.github/workflows/release.yml`（`v*` tag 触发，GPG 导入 + settings.xml heredoc + `mvn -Prelease deploy`，最小权限）。**首发为人工**：tag 推送前须完成 RELEASING.md §1 六项前置（命名空间/GPG key+公钥发布/四 Secrets/LICENSE/Portal 令牌/阿里云镜像迁出 POM）；autoPublish=false 至 staging 由 Portal 手动发布。
- [x] BOM 统一版本、全模块同版本演进有校验 — 父 POM `maven-enforcer-plugin` `requireSameVersions`（groupId io.github.chyuan-cuihongyuan，validate 阶段，全模块继承）；`mvn -o validate` 全 16 模块 `RequireSameVersions passed`。

## 备注

- **范围裁定（用户选择「仅发布工程」）**：ticket 14/15/17/18/19 收口备注里的「AutoConfiguration 装配归 ticket 20」未在本轮做——代码已设计好 `XxxModule.configure()` / builder 返回 `RuntimeConfig`、经 `RuntimeConfig.merge` 组合的入口，但目前 14 个模块均无 `@AutoConfiguration` / `.imports` / `@ConfigurationProperties` / `@ConditionalOnProperty`。starter pom 亦无依赖聚合。该 AutoConfig 层建议单列后续 ticket 推进（14 模块 AutoConfig + starter 聚合 + ApplicationContextRunner 装配测试）。
- **阿里云镜像泄漏（双轴复审 Standards 头号项）**：根 POM `<repositories>`/`<pluginRepositories>`（本机绕 repo.maven.apache.org TLS 指纹拦截用）会被写进已发布 POM、传染下游且 Central 会拒收。曾尝试 `flatten-maven-plugin`（oss / defaults / 显式 pomElements 三种配置）在发布时剥离 repositories——但均行为异常（oss 保留 repositories 却删 dependencyManagement 致 BOM 空壳；显式 `<scm>keep</scm>` 等不生效），在无法读官方文档（maven.apache.org 被拦截）且无法跑真实 deploy 校验的前提下，**发布一个误配的 flatten 风险高于收益**，故回退。改为在 RELEASING.md §1 第 6 项设为「硬性门禁」：首次发布前把镜像迁到 `~/.m2/settings.xml`（CI ubuntu-latest 直连 central）。彻底修在代码里的替代方案（迁 settings.xml + 同步改 CLAUDE.md 本地构建说明）留待用户确认是否扩张范围。
- **复审修复**：scm `<connection>` `git://`→`https://`（GitHub 2022 停用 git://）；javadoc 去 `failOnError=false`（避免静默发损坏的 javadoc jar）；RELEASING `versions:commit` 去 `-N`（免子模块 `.versionsBackup` 残留）。
- **本机验证（用户提供 JDK 21 ms-21.0.12）**：`mvn verify` 全 13 非 Docker 模块 BUILD SUCCESS（buzhou-store-jdbc / buzhou-store-redis / buzhou-observe-otel 用 Testcontainers 需 Docker，本机无 Docker，已单独 `test-compile` 通过）；`mvn -Prelease verify -Dgpg.skip=true` 验证 sources/javadoc 附加链路、每模块出齐 main+sources+javadoc。未验项仅剩：Testcontainers 测试（需 Docker）、gpg `.asc` 签名（未装 gpg）——交 CI 兜底。
