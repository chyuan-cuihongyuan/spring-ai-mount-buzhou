# Spec 147 — turn 内工具结果 memo（effort #102）

> wayfinder map：`.wayfinder102/MAP.md`（T501–T502）。借鉴：Hystrix request
> caching（请求作用域缓存）——同一轮内复读工具调用直接复用首轮结果。

## Problem Statement

模型在一轮内复读同一工具同一入参（常见于多步推理回查同一事实）：每次都真实
执行——下游承压、轮耗时增加、结果还可能因时序不同而轻微不一致。响应缓存
（spec 53）跨请求复用历史答案（TTL/容量/失效责任重）；「本轮内」这个最小
作用域反而没有轻量机制。

## Solution

`TurnMemo`（core/exec）+ `MemoizedToolCallback` 装饰器 + `TurnMemoHook`：

- **轮作用域**：hook beforeTurn（order 30）清零 memo 表——每轮一张白纸，
  零 TTL/容量/失效问题。
- **装饰器**：`wrap(callback, memo)`——call(args) 以 工具名+argsHash（与
  ToolCallLogEntry.argsHash 同口径）为 key 走 computeIfAbsent：首轮执行、
  轮内复读秒回同值（引用同一结果——一致性与省执行双得）。
- **失败不 memo**：异常原样上抛不入表（失败是可重试信号，不是可复用值）。
- 统计：`hitCount/missCount`（观测面）。
- 幂等契约：只包只读/幂等工具（复读复查型；写工具复读本就该被拦，见 superstep
  原子批/串行组）。

## User Stories

1. 作为模型，我复读检查同一事实时秒得首轮结果——轮内自洽且省时。
2. 作为宿主，我给只读查询工具包 memo，下游 QPS 随复读习惯自然下降。
3. 作为运维，hit/miss 比例告诉我模型的「复读率」——提示词优化信号。

## Implementation Decisions

- memo 表在 TurnMemo 实例（会话级对象），hook 持同引用轮清零——单实例单线程
  轮内访问，无需并发 map（ConcurrentHashMap 保守起见仍用——多工具并行批内
  复读可能并发 computeIfAbsent）。
- 装饰器透传 ToolDefinition（装配面零感知——RetryingToolCallback 同款）。

## Testing Decisions

- 复读单执行同值；异参各执行；afterTurn→下轮清零再执行；异常不 memo（再调重试）；
  统计计数；hook 清零接线（beforeTurn 后 hit 生效）。

## Out of Scope

- 跨轮 TTL memo（响应缓存域）；harness 内嵌接线；写工具复读拦截。

## Further Notes

- 去重三件套续：响应缓存（53）/ 语义缓存（55）/ 在飞合并（139）/ 轮内 memo（本轮）。
