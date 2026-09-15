---
id: T2625
title: 工具调用批规模直方的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: []
created: 2026-09-15
---

## Question

ToolBatchHistogram 的形状怎么裁决？（spec 1712 / effort #1712 / R13）（spec 1712 验收/裁决）

## Resolution

实例面桶式（ToolInputSizeHistogram 房规）：默认边界 {2,3,4,5}→5 桶（1/2/3/4/5+）+record <1 忽略+largestBatch 峰值——OpenAI 并行工具/vLLM batching 批规模遥测，lane 调参依据。
