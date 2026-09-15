---
id: T2882
title: 向量时钟偏序的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2881]
created: 2026-09-16
---

## Question]

因果三态在先后/并发/稀疏/退化/畸形五面下正确吗？（spec 1840 / effort #1840 / R41）

## Resolution

**VectorClockOrderTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=VectorClockOrderTest）：{n1:2,n2:1}×{n1:1,n2:2} 判并发；先/后
单向严格小；缺席按 0（空表先于在场）；相等与 null 退化 BEFORE；负分量
fail-fast。

