---
id: T2382
title: R16 泄漏疑似聚合接线的验证裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2381
created: 2026-09-15
---

## Question

N 会话第 16 轮：如何验收？

## Resolution

LeakSuspectHolderTest 两断言：复合 listener 三次 onLeak——宿主收 3 + 聚合排行
（2 键、同键 count=2/maxAge=300）；null 宿主复合仅聚合器收。
LeakSuspectAggregatorTest 4 用例零回归。
