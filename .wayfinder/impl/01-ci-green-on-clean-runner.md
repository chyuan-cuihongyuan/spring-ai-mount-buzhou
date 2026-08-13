# 01 — CI 在干净 GitHub runner 上稳定转绿

**Impl-of:** [`.wayfinder/SPEC.md`](../SPEC.md) · **maps-to decision:** [T1](../tickets/T1-ci-red-remotely-green-locally.md)

**What to build:** 从「GitHub badge=failing、本地 `mvn clean verify` 全绿」到「干净 runner 上 `mvn verify` 稳定绿」的端到端可信度修复。先取真实 CI 失败日志确认根因（[T2](../tickets/T2-spring-ai-native-vs-buzhou.md) 已锁定为**环境性 maven 缓存**：`setup-java` 的 `cache: maven` 残留 GA 前的否定解析标记 `.lastUpdated`），再修复（清缓存 / `-U` 强制更新 / 临时移除 `cache: maven` 之一或组合），push 验证 badge 转绿。**绝不加 milestone / snapshot 仓**——`spring-ai 2.0.0` / `spring-boot 4.1.0` 均已 GA、在 Maven Central，pom 无 `<repositories>` 是对的。修复后「core 鲁棒」的结论才建立在可复现证据之上。

**Blocked by:** 无 — 可立即开始（本环境无 `gh` / 未鉴权，取真实 CI 日志须请用户代取）。

**Status:** fix-applied (assignee: zcode) — 真实根因已坐实并修复；badge 转绿待用户 push（本环境无凭据）

- [x] 取到真实 CI 失败定位 — 用户贴 CI 日志 + 公开 check-run API（run 31622806373, sha cbbca3e2）：失败在 `buzhou-tools` `Build & test` step，exit 1
- [x] 根因坐实（**纠正 T2 缓存假设**）：`RunCommandToolTest.detachedChildHoldingPipeKilledAndOutputDrained` 在 **Linux CI 失败**——`sleep 60 & echo detached-done` 后台子进程被 reparent 到 init，`descendants()` 杀不掉、且 `readNbytes` 阻塞等 EOF → `detached-done` 输出丢失 → 断言失败。**T2 的「缓存」诊断被证伪；T10 的「真实缺陷」正确。**
- [x] 实施真实修复 — `RunCommandTool.readBounded` 改为 `available()` 非阻塞排空 + 主进程死后宽限即返回（不阻塞等 EOF）；测试改为断言「输出被捕获 + 限时完成」（见 commit）。pom 未动 `<repositories>`
- [x] ci.yml 复位 — 撤销此前基于错误 T2 诊断的 `cache: maven` 移除（缓存非根因、恢复 CI 速度），并升级 `actions/checkout@v4→v5` + `setup-java@v4→v5`（消 Node20/setup-java 弃用警告）
- [ ] push 后 CI badge 转绿 — ⏳ 待用户在已鉴权环境 push 后观测（本环境无凭据无法 push）
- [~] 本地验证 — JDK 21 `mvn -pl buzhou-tools -am test-compile` 全绿；`readBounded` 算法以独立用例验证（输出被捕获、~591ms、不悬挂）；`/bin/sh` 真实运行 + Linux reparent 行为须 CI 终验

## Progress (2026-08-13)

**根因纠正**：用户贴 CI 日志 + run 31622806373 确认失败在 `buzhou-tools`（非依赖解析、非缓存）。坐实为 `detachedChildHoldingPipeKilledAndOutputDrained` 的真实缺陷（Linux reparent + `readNbytes` 阻塞丢输出）——**T2 缓存诊断被证伪**，并发进程 [T10](../tickets/T10-fix-ci-os-specific-defect.md) 的「真实 OS/构建缺陷」方向正确。

**修复**：`RunCommandTool.readBounded` 重写（`available()` 非阻塞排空 + 死后宽限即返回，不阻塞等 EOF）+ 测试改为断言「输出被捕获 + 限时完成」。本机 JDK 21 编译通过、算法独立验证 PASS；Linux `/bin/sh` 运行时行为待 CI 终验。

**ci.yml**：撤销错误诊断下的 `cache: maven` 移除（恢复），升级 actions 至 v5（消弃用警告）。

**待用户**：在已鉴权环境 push → 观测 badge 是否转绿。若转绿则 01 闭合；若仍有别的 buzhou-tools 失败（多因），取新日志贴回续修。
