---
Type: task
Status: closed
---
## Question

ErrorBudget（桶环窗 + burn = errorRate/(1−SLO) + min-samples 门 + scope 256
折叠）+ ErrorBudgetHook（order 250 纯观察，131 同标记语义）+ ErrorBudgetHealth
（DOWN=燃尽超阈，无样本 UNKNOWN）+ ErrorBudgetProperties + 装配三 bean
（slo 未配不装配）。

## Resolution

done（2026-09-02）：impl-344；ErrorBudget 六用例绿（浮点严格相等改
isCloseTo——burn 除法表示误差）。
