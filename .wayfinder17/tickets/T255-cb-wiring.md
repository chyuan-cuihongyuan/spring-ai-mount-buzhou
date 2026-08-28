---
Type: task
Status: closed
blocked-by: T254
---
## Question

ModelCircuitBreaker 增可选 backend 构造参数（null=不共享）：transition(OPEN) → recordTrip；
transition(CLOSED from HALF_OPEN) → clear；admit() CLOSED 分支先查 activeTrip——活跃则
按共享 openedAt/cooldown 计算剩余冷却并以 OPEN 语义拒绝（本地状态机不动）；本地
OPEN/HALF_OPEN 分支零变化。

## Resolution

done（2026-08-29）：见 MAP Decisions 与 spec 57 对应节。
