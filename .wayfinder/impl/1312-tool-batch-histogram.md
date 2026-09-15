# impl 1312 — ToolBatchHistogram 批规模直方（R13 = effort #1712 / spec 1712 / T2625-T2626）

**What**：桶式 {2,3,4,5}→5 桶+largestBatch 峰值+非法忽略
**Why**：OpenAI 并行工具/vLLM batching 遥测——模型并行度画像
**Verify**：ToolBatchHistogramTest 3 断言 全绿。 **Status**：done（2026-09-15）
