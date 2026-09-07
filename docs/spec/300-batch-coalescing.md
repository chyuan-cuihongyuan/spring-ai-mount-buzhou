# Spec 300 — 批内工具调用合并接线（effort #300）

> wayfinder map：`.wayfinder300/MAP.md`（T591–T592）。借鉴：Hystrix request
> collapsing（spec 139 原语的装配收尾——B 会话 fog 种子第 1 项）。

## Problem Statement

模型在同一批工具调用里经常重复发同一调用（同工具同参），并行 fan-out 会
真实执行 N 次：下游 QPS 放大、批延迟取最慢执行、串行组/许可被重复占用。
在飞合并原语 `ToolCallCoalescer`（spec 139）已建但 standalone——执行脊柱
`HarnessToolCallingManager` 不认识它，装配缺口。

## Solution

`HarnessToolCallingManager` 批派发路径可选接入合并器（默认关 = 零行为变化）：

- `setBatchCoalescer(coalescer)`——经 `SessionAssemblyContext.toolManager()`
  注入（与 batchFeedbackPolicy / atomicBatchValidation 同通道）；null = 停用。
- 开启后批内派发走 `coalescer.submit(key, task, executor)`：键 = 工具名 +
  全参串（零碰撞）；首达者执行、同键位共享同一 Future；完成即忘（合并≠缓存）。
- 合并位共享值、独占 id：回喂位逐位重写为本位 `tool_call.id()`——协议要求
  每个调用位有 id 一致的回喂。
- `ToolCallCoalescer` 补取消桥接：共享 Future 被 cancel（Turn 超时路径）时
  中断底层执行任务——`CompletableFuture.cancel` 本身不触达任务，须显式桥接，
  否则超时取消语义在合并路径静默降级。
- 事件日志只记首执行位（合并位无独立执行，不虚构条目）。

## User Stories

1. 作为宿主，模型一批里连发 3 次同参目录查询——下游只执行 1 次，批延迟
   不再取 3 次执行的最大值。
2. 作为运维，`coalesced` 计数器即「批内重复率」观测面，直接量化收益。
3. 作为模型，合并位回喂 id 与我的调用位一一对应——协议不破、结果一致自洽。
4. 作为开发者，Turn 超时取消在合并路径依然中断底层执行——不引入挂死窗口。

## Implementation Decisions

- 默认关（`null` 合并器 = 既有 per-tool 行为），opt-in 与 139/122 家族一致。
- 键口径：工具名 + 全参串（放弃 hash——碰撞即错误共享，正确性优先）。
- 合并范围 = 本管理器实例（per-session）的在飞窗口——跨批跨轮天然覆盖，
  跨实例归 Out of scope。

## Testing Decisions

- 新增 `HarnessBatchCoalescingTest`（buzhou-core exec 包，风格随
   AtomicSuperstepBatchTest——manager 直驱 + dispatch 辅助）：
  批内同参 N 位执行一次且各位置回喂齐全、异参不合并、默认关双执行、
  合并位 id 重写、coalesced 计数、取消桥接中断底层任务。
- 外部行为断言（回喂内容/执行次数/计数），不断言内部线程结构。

## Out of Scope

- 跨实例合并（Redis 共享族）；结果缓存（183 TTL 已覆盖）；合并收益趋势面。

## Further Notes

- 去重家族接线状态：响应缓存（53 wired）/ 语义缓存（55 wired）/ 轮内 memo
  （147 wired）/ 工具 TTL（183 standalone→装配归后续 yml 配置面轮）/
  在飞合并（139 standalone→**本轮 wired**）。
