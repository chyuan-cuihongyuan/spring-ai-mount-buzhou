---
Type: task
Status: closed
---
## Question

巡检犬接锁：跳过零通知 null 哨兵 + skippedForLock 计数。

## Resolution:

done（2026-08-30）：impl-304；TurnStallWatchdog 5 参构造 + 红队 1 例 +
回归绿。
