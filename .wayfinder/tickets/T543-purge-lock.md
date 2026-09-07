---
Type: task
Status: closed
---
## Question

归档清理接锁：抢锁跳过哨兵 + finally 释放 + 零变化兼容。

## Resolution

done（2026-08-30）：impl-303；ArchivePurgeJob 5 参构造 + SKIPPED_LOCKED +
红队 1 例 + 清理/锁回归 7 绿。
