# Wayfinder Map — Buzhou turn 内工具结果 memo（effort #102，B 会话第 14 轮）

> B 会话第 14 轮。主题池「turn 内 memo」：模型同一轮里重复调相同工具相同入参
> （复读检查），现在每次都真实执行。借鉴 Hystrix request caching（请求作用域缓存）。

## Destination

TurnMemo（轮作用域 memo 表：beforeTurn 清零）+ MemoizedToolCallback 装饰器
（key=工具名+argsHash，computeIfAbsent；异常不 memo——失败可重试）。与
在飞合并（139）互补：那是并发折叠，这是轮内顺序复读去重。

## Notes

- 号段：B=奇数 spec（本轮 147）。
- 幂等契约同 RetryingToolCallback：只包只读/幂等工具。
- 轮清零 = 无 TTL 无容量问题（轮内键数天然有界于工具调用量）。

## Decisions so far

- 异常不 memo（语义结局失败是可重试信号，不是可复用值）。

## Not yet specified

- 跨轮 TTL memo（那是响应缓存 53 域）；memo 命中计数入遥测。

## Out of scope

- 沿用 #7–#101；HarnessToolCallingManager 内嵌接线。

## Tickets

- [x] [T501 TurnMemo + MemoizedToolCallback + TurnMemoHook](tickets/T501-turn-memo.md)（impl-286）
- [x] [T502 memo 回归（复读单执行/异参各执行/轮清零/失败不 memo）](tickets/T502-turn-memo-tests.md)（impl-286）
