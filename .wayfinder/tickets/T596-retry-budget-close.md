---
Type: task
Status: closed
---
## Question

yml 装配（backpressure.retry-budget.percent/min-balance，未配置零变化）+
拒绝语义三面回归（工具/core 装配/模型端到端）。

## Resolution

done（2026-09-01）：impl-325；BuzhouBackpressureProperties.RetryBudgetParams
（percent 默认 20 / min-balance 默认 10 / 全未配 = 关）+ autoconfig 装配 bean
（容器关闭清 holder）。RetryBudgetWiringTest / RetryBudgetAssemblyTest /
RetryBudgetAdvisorTest 三面绿。
