# Spec 139 — 在飞工具调用合并（effort #98）

> wayfinder map：`.wayfinder98/MAP.md`（T487–T488）。借鉴：Hystrix request
> collapsing / CDN request coalescing——同键<b>在飞</b>请求折叠为一次执行。

## Problem Statement

并行工具批内模型对同一工具发相同入参（双保险查询/重复检索），各执行各的——
下游承压翻倍、批耗时取最慢者；跨会话的突发同问（热点 FAQ 触发工具）同理。
这类<b>并发在飞</b>重复与历史缓存（spec 53/55 响应与语义缓存）是两个面：缓存
命中过去，合并不持历史、只折叠当下。

## Solution

`ToolCallCoalescer`（core/exec）：

- `submit(key, task, executor)`：首个到达者创建共享 `CompletableFuture` 并执行；
  并发同键者直接等同一 Future（执行一次、扇出同值）。计数 coalesced（被折叠数）。
- **完成即忘**：终态（成/败）后键即刻移除——无 TTL/容量/一致性问题（合并≠缓存）。
- **失败传播**：任务异常传给所有等待者（同一 Future 语义）；不重试（重试归
  spec 133 组合）。
- 线程安全；key 由调用方构造（建议 工具名 + argsHash——ToolCallLogEntry.argsHash
  同款口径）。

## User Stories

1. 作为宿主，并行批里的同工具同入参调用折叠成一次下游执行——批内零重复、
   批耗时不再被重复项拖长。
2. 作为下游服务，热点突发请求被在飞合并挡掉（coalesced 计数直接就是节省量）。
3. 作为宿主，不持历史意味着零一致性维护——同一问题稍后再问会真实再执行。

## Implementation Decisions

- ConcurrentHashMap + computeIfAbsent 原子建 Future；终态回调 remove（CAS 同款
  Future 才移除——防完成即忘与新建竞态误删）。
- 执行线程 = 调用方 executor（本类不自持线程）。

## Testing Decisions

- 慢任务 + 栅栏确保并发提交后才放行：执行次数=1、全部等待者同值、coalesced=N-1。
- 异键并发：各执行各的。
- 失败传播：所有等待者收同一异常。
- 完成即忘：第一波完成后第二波同键再执行（次数=2）。

## Out of Scope

- HarnessToolCallingManager 自动合并接线；窗口攒批（delay-batching）；历史缓存。

## Further Notes

- 去重三件套：响应缓存（53）/ 语义缓存（55）/ 在飞合并（本轮）。
