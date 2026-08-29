---
Type: task
Status: closed
---
## Question

幂等工具框架级重试：装饰器 + 指数退避 + 仅异常面。

## Resolution

done（2026-08-30）：impl-279；RetryingToolCallback.wrap（RetryPolicy 3 参 +
指数退避封顶 + 仅异常重试/错误文案不重试 + 耗尽上抛最后异常 + 重试计数）。
