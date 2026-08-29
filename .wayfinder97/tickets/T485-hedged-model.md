---
Type: task
Status: closed
---
## Question

模型对冲：主模型长尾等待中并发押注备模型，先回先得。

## Resolution

done（2026-08-30）：impl-281；HedgedChatModel implements ChatModel（hedgeDelay
后发备模型、输家取消、主快速失败转对冲、双败抛主异常、stream 委派主）+ 三计数。
