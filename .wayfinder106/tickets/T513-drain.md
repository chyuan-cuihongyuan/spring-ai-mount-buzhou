---
Type: task
Status: closed
---
## Question

单会话维护排水：拒新 + 在飞记账 + 等排空。

## Resolution

done（2026-08-30）：impl-290；SessionDrainCoordinator（Lease AutoCloseable +
排空 latch + 超时不死等）+ ErrorCode.SESSION_DRAINING。
