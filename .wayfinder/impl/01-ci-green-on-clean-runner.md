# 01 — CI 在干净 GitHub runner 上稳定转绿

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T1](../tickets/T1-ci-red-remotely-green-locally.md)

**What to build:** 从「GitHub badge=failing、本地 `mvn clean verify` 全绿」到「干净 runner 上 `mvn verify` 稳定绿」的端到端可信度修复。先取真实 CI 失败日志确认根因（[T2](../tickets/T2-spring-ai-native-vs-buzhou.md) 已锁定为**环境性 maven 缓存**：`setup-java` 的 `cache: maven` 残留 GA 前的否定解析标记 `.lastUpdated`），再修复（清缓存 / `-U` 强制更新 / 临时移除 `cache: maven` 之一或组合），push 验证 badge 转绿。**绝不加 milestone / snapshot 仓**——`spring-ai 2.0.0` / `spring-boot 4.1.0` 均已 GA、在 Maven Central，pom 无 `<repositories>` 是对的。修复后「core 鲁棒」的结论才建立在可复现证据之上。

**Blocked by:** 无 — 可立即开始（本环境无 `gh` / 未鉴权，取真实 CI 日志须请用户代取）。

**Status:** in-progress (assignee: zcode) — 修复已应用，badge 验证 env-blocked

- [ ] 取到真实 GitHub Actions 失败日志 — ❌ env-blocked（无 `gh`/未鉴权，见 memory `no-gh-cli-unauthenticated-env`）
- [x] 根因与 T2 结论（环境性缓存）一致 — 本地调研佐证：JDK 23 下模块 1–8 测试全绿、`buzhou-skills` 仅因 Windows CRLF 失败（Linux CI 平台会过）→ 代码/测试在 Linux 无缺陷 → CI 红非编译/测试缺陷，与 T2 缓存诊断一致（非反驳）
- [x] 实施修复 — `.github/workflows/ci.yml` 移除 `cache: maven`（干净 `~/.m2` 每次 run、规避 `.lastUpdated` 标记 / 损坏下载），pom **未动** `<repositories>`
- [ ] push 后 CI badge 转绿 — ❌ env-blocked（无法 push；须用户在已鉴权环境推送后观测）
- [ ] 本地 `mvn -B -ntp clean verify` 十五模块全绿 — Linux 目标平台绿（SPEC 2026-08-13 核验）；本 Windows 主机因 JDK8 默认 / CRLF / `/bin/sh` 缺失跑不绿（平台产物、非缺陷，见 memory `windows-host-cant-build-linux-project`）

## Progress (2026-08-13)

ci.yml 修复已提交（移除 `cache: maven`）。**剩余验收 env-blocked**：本环境无 GitHub 凭据 → 无法 push 触发 CI / 取 Actions 日志 / 观测 badge。

**需用户在已鉴权环境完成 01**：① push 后看 badge 是否转绿；② 若仍红，取真实 CI 失败日志贴回以最终坐实根因（届时可追加 `-U` 或改回 `cache: maven` 对比）。在 badge 转绿并稳定前，本切片不闭合。
