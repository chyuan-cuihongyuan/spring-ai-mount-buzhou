---
Type: task
Status: closed
---
## Question

RunRecoveryService.autoResumeAll：枚举 RUNNING 逐一试 restart(steal=false)；三态计数。

## Resolution

done（2026-08-29）：impl-215；4 例红队（接管/持锁跳过/零操作/失败隔离）。
