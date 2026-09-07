---
Type: task
Status: closed
---
## Question

命名泳道共享 Semaphore + 装饰器取还（超时抛错、异常归还）。

## Resolution

done（2026-08-30）：impl-300；ToolLaneRegistry（同名单例泳道）+
LaneLimitingToolCallback（tryAcquire 超时 + finally 归还）。
