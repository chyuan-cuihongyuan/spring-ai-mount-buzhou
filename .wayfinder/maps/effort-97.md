# Wayfinder Map — Buzhou 模型对冲请求（effort #97，B 会话第 9 轮）

> B 会话第 9 轮。主题池「模型对冲请求」：尾延迟（p99 长尾）现在只能靠降级链
> 串行等待主模型终态失败——借鉴 Google tail-tolerant / gRPC hedging：主模型超过
> 对冲延迟即并发发备模型，先回先得。

## Destination

HedgedChatModel（implements ChatModel，装饰器）：call() 主模型超 hedgeDelay 未回
即发备模型对冲、先回先得、输家取消；主模型快速失败也立刻转对冲；stream() 诚实
委派主模型（不对冲）。计数 primary-won/hedge-fired/hedge-won。

## Notes

- 号段：B=奇数 spec（本轮 137）。
- 与 spec 15 降级链互补：链管「终态失败后串行换人」，对冲管「长尾等待中并行押注」。
- 诚实边界：prompt 原样发给两模型（模型特定 options 兼容性归宿主）；
  对冲有双倍调用成本——hedgeDelay 应设在 p95 之上。

## Decisions so far

- 双败抛主模型异常（保既有错误语义与降级链触发口径）。

## Not yet specified

- ResilienceAdvisor 集成（按 EMA 自适应 hedgeDelay）；对冲事件 webhook。

## Out of scope

- 沿用 #7–#96；stream 对冲（流竞速复杂度不成比例）；N 路对冲（两路足够）。

## Tickets

- [x] [T485 HedgedChatModel（对冲竞速/输家取消/快速失败转对冲）](../tickets/T485-hedged-model.md)（impl-281）
- [x] [T486 对冲回归（先回先得/慢主快备/主快不冲/双败/流委派）](../tickets/T486-hedged-model-tests.md)（impl-281）
