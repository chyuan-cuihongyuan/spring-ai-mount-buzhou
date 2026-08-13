---
id: T32
title: core · 持久 Run 注册表 + 枚举续跑（lease 复用）
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

如何把既有「悬空调用 reactive 修复」升级为 **proactive 恢复**：持久化在途 Session/Turn、重启后枚举并安全续跑？事实源：Mastra（27,179★：`WorkflowsStorage.listWorkflowRuns(status,page)` + `persistWorkflowSnapshot` + `restart()`——已知坑：restart 可能重跑已完成 step、issue #5549 重启后不自动恢复、**无并发防护**）。

## 待定决策（研究推荐已备）

1. `RunRegistry`（listRuns 按 status 过滤+分页 / getRun / persistRunState，**以 Completed-Turn 为快照单元**）——采纳（快照粒度比 Mastra step 粗，但与 Buzhou 三级语义对齐）。
2. `RunHandle.restart()` = 从**最后 Completed-Turn 之后**续跑——采纳（不重跑已完结 Turn，规避 Mastra 重跑缺陷）。
3. **restart 前必须先拿到该 run 的租约**（复用既有 `InMemorySessionLeaseStore` SPI），拿不到即拒绝——采纳（补上 Mastra 未明的并发防护）。
4. InMemory + JDBC 双实现 + 自动恢复开关（启动时枚举续跑 vs 仅手动）——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §2.1（3–5 天，ROI 高；**Tier-2 单笔最高价值**，与 T33 绑定做）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §core-4**（用户常设授权 2026-08-14 ratify、可推翻）。RunRegistry 以 Completed-Turn 快照、restart 必须 lease、InMemory/JDBC 双实现。
