---
Type: task
Status: closed
---
## Question

文件咨询锁：原子抢锁 + 仅持有者释放 + 陈旧回收。

## Resolution

done（2026-08-30）：impl-302；`retention/AdvisoryFileLock` + 红队 3 例。
