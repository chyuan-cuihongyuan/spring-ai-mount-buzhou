---
Type: task
Status: closed
---
## Question

`ModelCostLedger.addListener` 监听缝（含 OVERFLOW 路径）+
`CostForecastHealth`（恒 UP + 速率/外推 details；无预算 UNKNOWN）+
yml `buzhou.budget.forecast.*` 装配。

## Resolution

done（2026-09-08）：impl-376；监听触发/details/UNKNOWN/yml 装配用例绿，
buzhou-core 全模块绿。
