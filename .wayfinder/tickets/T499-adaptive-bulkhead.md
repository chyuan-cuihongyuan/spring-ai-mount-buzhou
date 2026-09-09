---
Type: task
Status: closed
---
## Question

AIMD 动态并发闸：加性增/积性减/动态上限 fail-fast。

## Resolution

done（2026-08-30）：impl-285；AdaptiveBulkhead（per-agent 单锁 + Lease
AutoCloseable + snapshot 观测 + 调整计数）。
