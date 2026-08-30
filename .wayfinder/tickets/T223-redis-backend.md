---
Type: task
Status: done
blocked-by: T222-ratelimit-backend-spi.md
---
## Question

RedisRateLimitBackend（store-redis）：分钟固定窗 INCR/DECR + EXPIRE（首写）；超限回滚；
诚实差异入档（固定窗 vs 令牌桶整形；共享额度语义正确）。
