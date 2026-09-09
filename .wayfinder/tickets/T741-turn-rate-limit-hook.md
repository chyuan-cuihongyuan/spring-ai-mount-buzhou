---
Type: task
Status: closed
---
## Question

`TurnRateLimitHook` 惰性令牌桶（burst+permitsPerMinute、nanoSupplier
注入）+ key 策略（默认 sessionId/可插拔）+ block 语义。

## Resolution

done（2026-09-09）：impl-398；回填/独立/共享/部分回填用例绿。
