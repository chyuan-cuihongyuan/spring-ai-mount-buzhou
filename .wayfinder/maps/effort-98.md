# Wayfinder Map — Buzhou 在飞工具调用合并（effort #98，B 会话第 10 轮）

> B 会话第 10 轮。主题池「工具调用合并去重」：并行批里模型常对同一工具发相同入参
> （如双保险查询），现在各执行各的——同键在飞请求可合并执行一次、结果扇出。
> 借鉴 Hystrix request collapsing / request coalescing。

## Destination

ToolCallCoalescer（core/exec）：submit(key, task, executor)——同键在飞请求共享
同一 Future（执行一次、全部等待者得同值）；完成即忘（合并≠缓存——只压在飞抖）；
失败传播所有等待者；coalesced 计数。

## Notes

- 号段：B=奇数 spec（本轮 139）。
- 与 spec 133 重试/131 熔断正交；与响应缓存（spec 53）不同面：缓存命中历史值，
  合并只折叠并发在飞。
- 诚实边界：key 由调用方构造（建议 工具名+argsHash）。

## Decisions so far

- 完成即忘——不持有任何历史（无 TTL 无容量问题）。

## Not yet specified

- HarnessToolCallingManager 批内自动合并接线；窗口式批量（delay 攒批）。

## Out of scope

- 沿用 #7–#97；结果缓存；异步批量刷新。

## Tickets

- [x] [T487 ToolCallCoalescer（在飞同键共享 Future/完成即忘）](../tickets/T487-coalescer.md)（impl-282）
- [x] [T488 合并回归（单次执行扇出/异键各执行/失败传播/完成即忘）](../tickets/T488-coalescer-tests.md)（impl-282）
