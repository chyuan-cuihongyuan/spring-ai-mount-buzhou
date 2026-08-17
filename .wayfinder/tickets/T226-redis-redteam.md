---
Type: task
Status: done
blocked-by: T223-redis-backend.md
---
## Question

对抗：Redis 断连语义（fail-fast 上抛带修法——不静默 fail-open）；并发扣减竞差（固定窗
INCR 原子性断言）；跨进程时区/时钟无关性（窗口键 UTC）。
