---
Type: task
Status: closed
---
## Question

superstep 原子批：任一入参校验未过/工具缺失时整批不派发。

## Resolution

done（2026-08-30）：impl-271；HarnessToolCallingManager 增 atomicBatchValidation
开关（默认关）+ atomicPreflight 前检；ToolCallOutcome 增 BATCH_ABORTED。
