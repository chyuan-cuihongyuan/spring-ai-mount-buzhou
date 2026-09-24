---
id: T6195
title: S 会话 S48 周期对账的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

Wave 8 五件怎么对账收口？（spec 5047 / effort #5047 / S48）

## Resolution

快照 1203→1208 补登（XorFilter/SegmentTree/RobinHoodHashTable/
CrdtPnCounter/TinyLfuAdmission）；api-surface.md 同步；CONTEXT
计数对齐；全仓 verify 三门 + 台账核账（5000–5046 零缺位）。
