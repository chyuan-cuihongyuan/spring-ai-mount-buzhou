---
Type: task
Status: closed
---
## Question

在飞同键工具调用合并：一次执行、扇出同值、完成即忘。

## Resolution

done（2026-08-30）：impl-282；ToolCallCoalescer（computeIfAbsent 共享 Future +
终态 CAS 移除 + coalesced 计数 + 失败传播）。
