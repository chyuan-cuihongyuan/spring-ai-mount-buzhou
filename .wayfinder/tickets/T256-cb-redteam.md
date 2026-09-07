---
Type: task
Status: closed
blocked-by: T255
---
## Question

红队/单测：双 breaker（A/B）共享 Redis 后端——A 跳闸后 B 的 CLOSED 分支拒绝（retryIn
按共享 openedAt）；A/B 任一探测达标 clear 后双方放行；冷却期满（可注入 Clock/短 TTL）
双方转探测；无后端（null）行为与现状全同（回归钉住）；事件口径不变（call-rejected
state=OPEN）。

## Resolution

done（2026-08-29）：见 MAP Decisions 与 spec 57 对应节。
