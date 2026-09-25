---
id: T6278
title: T 会话 T39 Weighted Reservoir Sampler 加权蓄水池采样的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6277]
created: 2026-09-26
---

## Question

T39 合同怎么逐一验绿？（spec 6039 / effort #6039 / T39）

## Resolution

**验证通过**：WeightedReservoirSamplerTest 五测全绿——500 项
池 10 守恒；百万权重项驻留；同种子同样本；欠容量全保留；
fail-fast。
