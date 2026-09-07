---
Type: task
Status: closed
---
## Question

per-tool 探针注册 + 聚合探测 + 状态翻转通知。

## Resolution

done（2026-08-30）：impl-295；ToolHealthProber（探针异常=DOWN 不上抛、
consecutiveDown 侧写、翻转才通知、DOWN 计数）。
