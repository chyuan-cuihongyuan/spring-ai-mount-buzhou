---
id: T5070
title: Q 会话 R35 Croston 预测器的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5069]
created: 2026-09-18
---

## Question

R35 合同怎么逐一验绿？（spec 3034 / effort #3034 / R35）

## Resolution

**验证通过**：CrostonForecasterTest 七测全绿——周期 3 尺寸 5 收敛
5/3±0.1、全零 100 期 NaN、首非零直接初始化、率等价性（4@2 期与
8@4 期同率 ~2 形状不变性）、α=0.5 手算 9/1.5=6.0、计数对账、
α/负需求四路 fail-fast。
