---
Type: task
Status: closed
---
## Question

TokenBudgetHook 可选注入（既有构造零改动、null 旧行为）+ bean 恒在 +
listener 装配。

## Resolution

done（2026-09-08）：impl-390；接线差异断言+装配用例绿，buzhou-core
全模块绿。
