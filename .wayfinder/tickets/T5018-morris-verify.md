---
id: T5018
title: Q 会话 R9 Morris 近似计数的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5017]
created: 2026-09-18
---

## Question

R9 合同怎么逐一验绿？（spec 3008 / effort #3008 / R9）

## Resolution

**验证通过**：MorrisCounterTest 七测全绿——零态 0、首计数必中
（v=0 概率 1）、500 件×64 增量系综均值 [0.8n,1.25n]（无偏证据）、
2000 增量单调不降、1000 增量指数 ≤15（亚对数主张）、归零、同种子
轨迹回放一致。
