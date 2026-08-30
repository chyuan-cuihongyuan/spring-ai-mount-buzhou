---
Type: task
Status: closed
---
## Question

原子批四象限回归：开-违规中止整批 / 开-全过正常执行 / 开-工具缺失中止 / 默认关行为不变。

## Resolution

done（2026-08-30）：impl-271；AtomicSuperstepBatchTest 四测试 + 事件日志 BATCH_ABORTED
断言全绿。
