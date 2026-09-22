---
id: T2984
title: 分位数聚合偏差审计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2983]
created: 2026-09-23
---

## Question)

偏差在经典/容差/畸形下正确吗？（spec 1891 / effort #1891 / R92）

## Resolution`

**QuantileAggregationBiasTest 4 用例全绿**（mvn -pl buzhou-core
test -Dtest=QuantileAggregationBiasTest）：经典 {100,500} naive=300、
真值 500 偏差 −0.4；容差 ±10% 两侧行为；无偏 0.0 判定；畸形三型
fail-fast。
