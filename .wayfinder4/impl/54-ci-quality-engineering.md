# 54 — CI 质量工程

**What to build:** CI 产出覆盖率报告（jaCoCo 全模块）、spotbugs 与 CodeQL 观测段；workflow 有并发取消与超时；action 版本统一；仓库带 maven-wrapper。

**Blocked by:** 52-config-metadata-infra

**Status:** ready-for-agent

- [ ] jaCoCo pluginManagement+全模块；CI 上传报告
- [ ] spotbugs CI job（观测不卡门）+ CodeQL workflow
- [ ] ci.yml concurrency/timeout/报告上传；action 版本统一 v5
- [ ] mvnw + wrapper.properties；CI/README 构建命令对齐
