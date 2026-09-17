---
id: T5076
title: Q 会话 R38 自相关的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5075]
created: 2026-09-18
---

## Question

R38 合同怎么逐一验绿？（spec 3037 / effort #3037 / R38）

## Resolution

**验证通过**：AutocorrelationTest 六测全绿——交替列 lag1=−1/
lag2=+1 恰值、ramp 惯性（lag1>0.95/lag10>0.6）、白噪 500 点
lag1..5 全 |r|<0.15、正弦周期自锁定（lag10=+1/lag5=−1 半周期
反相）、常数 NaN、四路 fail-fast。
