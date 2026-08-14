# 54 — CI 质量工程

**What to build:** CI 产出覆盖率报告（jaCoCo 全模块）、spotbugs 与 CodeQL 观测段；workflow 有并发取消与超时；action 版本统一；仓库带 maven-wrapper。

**Blocked by:** 52-config-metadata-infra

**Status:** done

- [ ] jaCoCo pluginManagement+全模块；CI 上传报告
- [ ] spotbugs CI job（观测不卡门）+ CodeQL workflow
- [ ] ci.yml concurrency/timeout/报告上传；action 版本统一 v5
- [ ] mvnw + wrapper.properties；CI/README 构建命令对齐


## Done

commit: 见 git log（impl/54）。验证：全仓 mvn clean verify 绿 + 14 个模块 jaCoCo 报告生成（site/jacoco/index.html×14、jacoco.exec×15）+ mvnw 3.9.9 可用。
落地：jaCoCo 0.8.13 根 pom pluginManagement+全模块激活（prepare-agent+verify report；surefire argLine 改 @{argLine} 迟占位与 agent 共存）；CodeQL workflow（java，PR/main+weekly）；spotbugs weekly workflow（观测期 || true + 报告归档）；ci.yml 加固（concurrency cancel-in-progress、timeout 40min、surefire/jacoco 报告上传）；action 版本统一（nightly/release 的 checkout/setup-java/setup-node → v5）；maven-wrapper（mvnw + 3.9.9）。注记：覆盖率阈值卡线/checkstyle/pmd/spotless/多 JDK/failsafe 不做（T79 决议）。
