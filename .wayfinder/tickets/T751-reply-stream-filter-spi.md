---
Type: task
Status: closed
---
## Question

core 回复流出站过滤 SPI：`StreamTextFilter`（filter/flush，每轮新建）+
`BuzhouHook.replyStreamFilter()` 默认方法 + `HookChain.newReplyFilters()`
收集 + DefaultAgentSession 流式（map+concatWith flush）与 chat（filter+
flush 整段）两缝接线；无 filter 零开销零行为变化。

## Resolution

done（2026-09-11）：impl-403；窗口跨界/flush 排空/零行为回归用例绿。
