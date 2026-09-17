---
id: T5054
title: Q 会话 R27 高斯采样器的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5053]
created: 2026-09-18
---

## Question

R27 合同怎么逐一验绿？（spec 3026 / effort #3026 / R27）

## Resolution

**验证通过**：GaussianSamplerTest 七测全绿——10 万样本矩收敛
（0±0.02/1±0.02）、N(10,2²) 平移缩放、5 万对双分量皆标准正态、
1σ 68.27%±1% 与 2σ 95.45%±1% 贴理论、σ=0 恒常量、同种子回放、
负 σ/null 三路 fail-fast。
