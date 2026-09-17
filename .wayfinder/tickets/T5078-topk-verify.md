---
id: T5078
title: Q 会话 R39 top-k 采样的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5077]
created: 2026-09-18
---

## Question

R39 合同怎么逐一验绿？（spec 3038 / effort #3038 / R39）

## Resolution

**验证通过**：TopKSamplerTest 八测全绿——k=1 千抽恒 argmax、
T=0.01 贪心 >98%、T=10000 三项 1/3±0.02、读数 {0.8,0.2} 手算、
截断集外恰零、−∞ 两千抽永不中、同种子回放、四路 fail-fast。
