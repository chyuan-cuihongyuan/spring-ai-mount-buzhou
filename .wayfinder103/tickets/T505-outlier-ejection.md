---
Type: task
Status: closed
---
## Question

连错端点驱逐出备选池 + 窗口复池 + 过滤视图。

## Resolution

done（2026-08-30）：impl-287；ModelOutlierEjection（per-model 单锁 + Clock
注入 + ejectedModels 名单 + 驱逐计数）。
