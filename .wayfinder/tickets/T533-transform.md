---
Type: task
Status: closed
---
## Question

工具结果入上下文前的宿主声明式变换（fail-open）。

## Resolution

done（2026-08-30）：impl-298；TransformingToolCallback（变换异常/null/空白
一律回退原文；定义透传；与 memo/retry 可叠加）。
