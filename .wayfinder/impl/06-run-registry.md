# 06 — core · 持久 Run 注册表 + 枚举续跑（lease 门）

**What to build:** 进程重启后枚举在途 run 并安全续跑：以 Completed-Turn 为快照单元的 Run 注册表，restart 从最后 Completed-Turn 之后续跑且必须先拿到租约——把悬空修复从 reactive 升级为 proactive 恢复。

**Blocked by:** None — can start immediately.

**Status:** done（2026-08-14：RunRegistry SPI + InMemory/JDBC 双实现 + RunStateTrackerHook + RunRecoveryService（lease 门）+ RecoverySupport.attach 三件套；schema 三方言增 buzhou_run_registry 表；RunRecoveryEndToEndTest 3 例 + JdbcRecoveryStoresTest；spec 05 增恢复章节）

- [ ] `RunRegistry`：listRuns（按 status 过滤+分页）/ getRun / persistRunState，快照单元=Completed-Turn
- [ ] `RunHandle.restart()`=从最后 Completed-Turn 之后续跑（不重跑已完结 Turn）
- [ ] restart 前必须先拿到该 run 租约（复用既有 SessionLeaseStore SPI），拿不到即拒绝
- [ ] InMemory + JDBC 双实现，沿用 store 契约测试范式
- [ ] 自动恢复可开关（启动枚举续跑 vs 仅手动）
- [ ] 端到端：模拟崩溃→重启→枚举→续跑→会话完整
- [ ] spec（05 或新增恢复章节）同步

> spec 12 §core-4；[T32](../tickets/T32-core-run-registry.md)。源：mastra 27,179★（listWorkflowRuns/persistWorkflowSnapshot/restart——规避其重跑与无并发防护缺陷）。
