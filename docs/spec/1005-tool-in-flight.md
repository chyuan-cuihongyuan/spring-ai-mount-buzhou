# 1005 — 工具在飞并发水位读面

> 来源：J 会话第 6 轮 = effort #1005（[T1461](../../.wayfinder/tickets/T1461-tool-in-flight-shape.md) / [T1462](../../.wayfinder/tickets/T1462-tool-in-flight-verify.md) / impl 758）。借鉴：Go runtime `runtime.NumGoroutine` 水位读数 / Hystrix 并发执行观测（current/peak 双水位）。

## Problem Statement

并发**限流**面已三处（spec 05 每轮信号量、LaneLimitingToolCallback 泳道、spec 84 舱），但并发**测量**面为零：spec 127 是 Turn 级跨实例聚合，单工具进程内在飞水位不可见——「哪个工具卡住了（在飞长期不归零）/ 峰值并发到过多少（容量依据）」无读数。

## 目标

- 新公共类 `ToolInFlight`（core.exec，api 面）：
  - 静态 `enter(toolName)` 发 `Lease`（AutoCloseable；close 恰一次——AtomicBoolean 守卫，重复 close 无害）；
  - 每工具 `current/peak/total`（peak 只增不降；total 累计开出租约数）+ 全局 `currentTotal/peakTotal`；
  - `snapshot()` → 嵌套 record `Snapshot(currentTotal, peakTotal, perTool: Map<String, PerTool>)` 不可变快照；
  - `reset()` 测试注入点（BuzhouMetricsHolder 进程态先例）。
- 接线：`HookedToolCallback` 在 delegate.call 外围 try/finally 进出（与 timer 同点）。

## 兼容性

纯增量读面：限流/超时/聚合语义零变化；热路径增两次原子操作（enter/leave）；per-tool map 以工具目录规模为界（ToolTimingAggregator per-tool map 同先例，不违无界纪律）。

## Out of Scope

- 跨实例聚合（spec 127 已有 Turn 级范式，工具级留 fog）。
- 在飞时长分布（spec 108 timer/700 聚合已覆盖时长轴——本轮只做并发轴）。
