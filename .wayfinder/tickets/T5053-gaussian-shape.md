---
id: T5053
title: Q 会话 R27 高斯采样器的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

均匀源怎么显式可审计地造高斯样本？（spec 3026 / effort #3026 / R27）

## Resolution

**GaussianSampler（core/policy，纯函数）**：Box-Muller 极坐标变换
——一次产一对独立标准正态（samplePair 天然单位），无缓存口径不
藏状态；u₁=1−nextDouble()∈(0,1] 免 log(0)；平移缩放 N(μ,σ²)+
σ=0 退化常量+注入回放。JDK nextGaussian 黑盒无口径病的显式件。
