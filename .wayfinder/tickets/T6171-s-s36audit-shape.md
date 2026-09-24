---
id: T6171
title: S 会话 S36 周期对账的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

Wave 6 五件与 S31 换静脉勘误怎么对账收口？（spec 5035 /
effort #5035 / S36）

## Resolution

快照 1193→1198 补登（WoundWaitGate/LeveledCompaction/
BPlusTree/ContentDefinedChunking/RadixTree）；api-surface.md
+5 行+勘误补 SegmentLog 行；CONTEXT 计数对齐；旧 jumphash
票清除；全仓 verify 三门 + 台账核账（5000–5034 零缺位）。
