# Spec 1712 — 工具调用批规模直方（effort #1712，R13）（effort #1712，R13）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2625–T2626，impl 1312，impl OpenAI 并行工具调用 / vLLM batching 的批规模遥测）。借鉴：单轮内工具调用数无画像：1=串行、>1=并行——模型会不会/多常用并行工具调用，无读数则 lane 配置（ToolLaneRegistry）与并发上限调参全凭猜。

## Problem Statement

`ToolBatchHistogram`（core/hook，实例面线程安全）：默认边界 {2,3,4,5} → 5 桶（1/2/3/4/5+），ToolInputSizeHistogram 同款桶式房规；record(batchSize)（<1 忽略）+bucketCounts()+total()+largestBatch()。

## Solution

作为 lane 调参者，5+ 桶占 30% → 模型重度并行，lane 容量上调。

## User Stories

1. 17120
2. 17121
3. 17122

## Implementation Decisions

- 17123

## Testing Decisions

- 17124

## Out of Scope

- 17125

## Further Notes

- 17126
