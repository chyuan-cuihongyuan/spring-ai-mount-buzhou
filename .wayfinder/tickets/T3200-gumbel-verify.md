---
id: T3200
title: Gumbel-max 采样器的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3199]
created: 2026-09-17
---

## Question

GumbelMaxSampler 合同（回放/偏斜/禁选/均匀/对账/畸形）怎么钉住？（spec 2049 / effort #2049 / R50）

## Resolution

**六用例全绿**（首跑编译红两修：测试残留占位实验行、SplittableRandom
包名 java.util 非 java.util.random；修后 6/6）：同种 42 双采样器 20
步全同 / 强偏 (10,0,0) 主位 >900/1000 / -∞ 禁选 500 次零中 / 均匀四
桶各 ∈[20%,30%] / softmax(ln2,0) 频率 (2/3,1/3)±0.04 对账 / 畸形五型
（null rng、null/空 logits、NaN、trials 0）fail-fast。
