---
Type: task
Status: done
blocked-by: T223-redis-backend.md
---
## Question

starter 聚合装配：store.type=redis 且 rate-limit 配置 → Redis 后端注入 ResilienceModule；
否则内存（默认零变化）；多实例 WARN 消除路径（Redis 后端下限流不再单进程告警）。
