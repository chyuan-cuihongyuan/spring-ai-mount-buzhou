---
id: T2804
title: Spill PSI 失速读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2803]
created: 2026-09-16
---

## Question

PSI 双档账目在混合/空闲/空表/畸形/并列峰值五类输入下行为正确吗？（spec 1801 / effort #1801 / R2）

## Resolution

**SpillPressureStallTest 5 用例全绿**（mvn -pl buzhou-spill test
-Dtest=SpillPressureStallTest）：some/full 分档账目+峰值窗；空闲窗只进分母；
空表与 null 同哨兵；畸形（负数/stalled>active）fail-fast；峰值并列取失速面
更宽窗。

