---
id: T6183
title: S 会话 S42 周期对账的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

Wave 7 五件怎么对账收口？（spec 5041 / effort #5041 / S42）

## Resolution

快照 1198→1203 补登（EpochReclamation/DistributedSnapshot/
ShuffleSharding/SlruCache/BlockedSlidingCounter）；
api-surface.md 同步；CONTEXT 计数对齐；全仓 verify 三门 +
台账核账（5000–5040 零缺位）。
