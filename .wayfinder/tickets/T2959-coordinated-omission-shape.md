---
id: T2959
title: 协同遗漏校正的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-23
---

## Question)

慢响应掩盖的发送机会怎么补账？（spec 1879 / effort #1879 / R80）

## Resolution`

**HdrHistogram expectedInterval 阶梯语义纯计算
`CoordinatedOmissionAudit`（core/metrics）**：correctedSampleCount
（观测 1+阶梯补记，450/100→4）+ omittedCount（补记−1——被掩盖的
发送机会）+ blindWindow（latency−interval 静默窗）+ coverageRatio
（原始覆盖真实需求比例）。阶梯只计数不物化；畸形四型 fail-fast。
