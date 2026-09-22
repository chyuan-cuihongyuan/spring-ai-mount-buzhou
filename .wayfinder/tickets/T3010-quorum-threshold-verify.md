---
id: T3010
title: 票数下限判定的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3009]
created: 2026-09-23
---

## Question)

票数下限在多数派/拜占庭/畸形下正确吗？（spec 1904 / effort #1904 / R105）

## Resolution`

**QuorumThresholdTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=QuorumThresholdTest）：多数派 3/4/5→2/3/3；拜占庭容忍
3/4/7→0/1/2；最小规模 f=1→4、f=2→7；畸形两型 fail-fast。
