---
id: T2150
title: 拒绝榜计数/排序/清榜不清桶的验证
type: task
status: closed
assignee: zcode-l
blocked-by: T2149
created: 2026-09-14
---

## Question

如何证明 per-key 计数、排序与清榜语义？

## Resolution

**用户常设授权 AFK（可推翻）**

`TurnRateLimitBlockedSnapshotTest` 三测全绿（`mvn -pl buzhou-core -am test`）：被拦 key 计数正确+放行 key 不入榜（4 连发拦 3）；hot=3/warm=cool=2 降序典序；**reset 清榜不清桶**（时间推进回填后照常放行——评审修正：初版 0.001/分钟策略回填量算错，改 60/分钟策略+10s 推进）。TurnRateLimitHookTest 回归 4 测绿。
