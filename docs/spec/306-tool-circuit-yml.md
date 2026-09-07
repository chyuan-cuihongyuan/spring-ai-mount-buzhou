# Spec 306 — 工具熔断 yml 装配（effort #306）

> wayfinder map：`.wayfinder/maps/effort-306.md`（T603–T604）。借鉴：resilience4j
> （spec 131/165 原语装配面——fog 227「新 hook 配置面族」首项）。

## Problem Statement

工具熔断 hook 已建但只能编程注册：yml 声明面缺失，运维调参（窗口/阈值/冷却/
半开名额）需要改代码。

## Solution

`buzhou.tools.circuit` 属性组（enabled 默认 false；window-size 默认 20 /
failure-rate-threshold-percent 默认 50 / cooldown 默认 60s / half-open-trials
默认 3；非法值启动即红带修法）。enabled=true → `ToolCircuitBreakerHook` bean，
`BuzhouHook` 类型由既有 `List<BuzhouHook>` 收集面自动并入 RuntimeConfig。

## User Stories

1. 作为运维，yml 四键调熔断参数——窗口阈值随故障画像走，无代码变更。
2. 作为宿主，hook bean 自动收集进会话装配——与既有 hook 面同通道。

## Implementation Decisions

- 校验同 `ToolCircuitBreaker.Config` 口径（装配层归一 fail-fast）。

## Testing Decisions

- `ToolCircuitAssemblyTest`：默认关无 bean；enabled 装配（且是 BuzhouHook）；
  参数绑定生效；非法值启动红。

## Out of Scope

- 跨实例共享后端；per-tool 参数覆盖。

## Further Notes

- 配置面族（fog 227）：合并器（300 已接线）/ 健康探测（305）/ **熔断（本轮）**；
  TTL 缓存、结果变换、泳道装饰器 yml 面续后。
