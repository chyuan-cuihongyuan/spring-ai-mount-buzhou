---
Type: task
Status: closed
---
## Question

RetryBudget 接入模型/工具两条重试路径（tryAcquire 拒即原错上抛 + 每逻辑调用
deposit；holder 载体零签名变更）。

## Resolution

done（2026-09-01）：impl-325；`RetryBudgetHolder`（backpressure）+ 
ResilienceAdvisor 拒绝语义（retry-exhausted 事件 + retry-budget-denied 计数）
+ RetryingToolCallback 三参 wrap（两参默认取 holder）。
