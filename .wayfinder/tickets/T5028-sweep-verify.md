---
id: T5028
title: Q 会话 R14 扫线最大并发的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5027]
created: 2026-09-18
---

## Question

R14 合同怎么逐一验绿？（spec 3013 / effort #3013 / R14）

## Resolution

**验证通过**：SweepLineIntervalsTest 九测全绿——三区间峰 2@5 手算、
相接恒 1（半开语义对照）、嵌套峰 3@4、全不相交峰 1@0、空集三读
哨兵、零宽贡献零、start>end/null fail-fast、乱序输入等值、双峰
并列取最早首发点。
