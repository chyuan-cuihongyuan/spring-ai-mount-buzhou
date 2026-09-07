# Spec 305 — 工具健康探测装配（effort #305）

> wayfinder map：`.wayfinder/maps/effort-305.md`（T601–T602）。借鉴：Consul health
> check（spec 165 原语装配收尾——探测原语 standalone 无装配面）。

## Problem Statement

工具健康探测原语（165）已建但宿主要手工构造与调度：无 bean 注入点、无 yml
开关、无健康面——探测能力不可装配。

## Solution

`buzhou.tools.health` 属性组（enabled 默认 false / interval 默认 30s）：

- enabled=true → `ToolHealthProber` bean（destroyMethod=stop）+ 周期自调度 +
  状态翻转计数 `buzhou.tools.health.flipped{tool,to}`。
- 随附 `ToolHealth`（BuzhouHealth）：机制面恒 UP（严格口径——外部工具 DOWN
  不等于工具机制失能），details = {registered, down 列表}。
- prober 补 `lastKnown()`：最近已知状态快照（健康面读取不主动探测；未探视
  注册项记 UP——无 DOWN 证据不报 DOWN）。

## User Stories

1. 作为宿主，注册探针后 yml 开关即得周期探测 + 健康面——无手工调度代码。
2. 作为运维，down 列表在健康端点直接可见（有界详情）。

## Implementation Decisions

- 探针注册保持编程式（宿主供 Callable<Boolean>——框架不知道怎么探）。

## Testing Decisions

- `ToolHealthAssemblyTest`：默认关零 bean；enabled 装配 prober + ToolHealth；
  DOWN 工具进 details 且机制面仍 UP；lastKnown 不触发探测。

## Out of Scope

- 探针自动发现；跨实例探测共享。

## Further Notes

- 配置面族进度（fog 227）：健康探测 ✅ → 熔断（306）→ 合并器已接线（300）
  → TTL 缓存/变换器/泳道装饰器 yml 面（后续轮）。
