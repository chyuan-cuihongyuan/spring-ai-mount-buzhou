---
Type: task
Status: closed
---
## Question

forkFromTurn 接口（default UOE）+ DefaultAgentRuntime 前缀复制实现。

## Resolution

done（2026-09-01）：impl-334；turnSeq ≤ upToTurn 前缀过滤 + Summary 不复制
（未来泄漏防护）+ fork 监听器/事件管线复用（payload 加 upToTurn）。
