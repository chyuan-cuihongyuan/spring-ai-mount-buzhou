---
Type: task
Status: closed
---
## Question

per-tool 熔断状态机：失败率滑窗跳闸 / 冷却 / 半开探测恢复。

## Resolution

done（2026-08-30）：impl-278；ToolCircuitBreaker（CLOSED→OPEN→HALF_OPEN→CLOSED，
计数环形滑窗 + 时钟注入 + snapshot 观测面）。
