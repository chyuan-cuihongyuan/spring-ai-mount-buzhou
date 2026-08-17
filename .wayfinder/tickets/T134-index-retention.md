---
Type: task
Status: closed
---
## Question

索引 CLOSED 行保留策略（effort #7 fog）：CLOSED/DELETED 行无限累积。淘汰口径？

## Resolution

AFK 自决：惰性清扫而非定时任务。`SessionIndexStore` 增 default `int purgeOlderThan(Instant cutoff, int limit)`（删除 lastActive < cutoff 且 status != ACTIVE；默认 no-op 返回 0，内存/JDBC/Redis 覆写）；调用方为 SessionIndexObserver.onOpen 时附带清扫（概率 1/64 触发、上限 256 条——免热路径开销）；保留期可配 `buzhou.index.closed-retention`（默认 30d，-1 永久）。产 spec 37 §C + impl-107。
