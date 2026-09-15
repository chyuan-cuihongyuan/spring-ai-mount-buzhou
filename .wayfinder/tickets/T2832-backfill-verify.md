---
id: T2832
title: 缺口回填计划的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2831]
created: 2026-09-16
---

## Question

缺口单在合并/边界/完整/哨兵/畸形五面下正确吗？（spec 1815 / effort #1815 / R16）

## Resolution

**GapBackfillPlannerTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=GapBackfillPlannerTest）：中段缺口 (3,5)/(8,8) 两段合并；首尾缺口
入账；完整/全缺/乱序重复容忍；空区间哨兵；区间倒挂/越界/null 元素
fail-fast。

