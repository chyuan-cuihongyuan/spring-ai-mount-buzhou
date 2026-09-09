---
Type: task
Status: closed
---
## Question

会话级连败隔离：阈值跳闸 + 指数退避 + 到时自动解除。

## Resolution

done（2026-08-30）：impl-284；SessionQuarantine（per-session 状态机，Clock 注入，
snapshot）+ ErrorCode.SESSION_QUARANTINED。
