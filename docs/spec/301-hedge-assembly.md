# Spec 301 — 对冲装配面（effort #301）

> wayfinder map：`.wayfinder301/MAP.md`（T593–T594）。借鉴：gRPC hedging
> （spec 137 原语的装配收尾——原语已建 standalone，yml 声明后自动生效）。

## Problem Statement

`HedgedChatModel`（spec 137）管「长尾等待中并行押注」，但宿主要手工构造并
塞进装配位：yml 无声明面、Spring 容器里主模型按类型注入位拿不到对冲装饰器
——能力存在却不可装配。

## Solution

`buzhou.resilience.hedge` 属性组 + 装配 bean：

- `enabled`（默认 false）、`primary-model`（主模型 bean 名，开启时必填）、
  `model`（对冲模型 bean 名，必填、不得与主同名）、`delay`（默认 200ms，
  建议设在主模型 p95 之上）。
- `hedge.enabled=true` 时注册 `@Primary` 的 `buzhouHedgedChatModel`：按类型
  取 ChatModel 的注入位（含 Spring AI ChatClient.Builder 装配）升为对冲
  装饰器；按名注入（fallback/shadow 解析）不受影响。
- 未命中 bean 名 fail-fast（带可用名清单）；同名自冲/空名/非正 delay
  启动即失败。
- 对冲专用虚拟线程执行器独立 bean（`shutdown` 随容器关闭）。

## User Stories

1. 作为宿主，yml 三行（enabled + 两个 bean 名）即得对冲——无需改代码。
2. 作为运维，`delay` 直接对齐主模型 p95 观测，无需重新编译。
3. 作为开发者，bean 名拼错启动即红（带可用名清单），不静默失效。

## Implementation Decisions

- 装配形态取新增 `@Primary` bean 而非 BeanPostProcessor 改内核——显式按名
  注入零影响；诚实边界：开启后宿主不得再自标 `@Primary` ChatModel。
- 执行器独立 bean（不与 core 共享——对冲线程生命周期随容器）。

## Testing Decisions

- `HedgeAssemblyTest`（ApplicationContextRunner，对齐
  `BuzhouResilienceAutoConfigurationTest` 先例）：默认关无 bean；yml 绑定
  + @Primary 升位 + 慢主快冲端到端先回先得；未命中 fail-fast；同名/空名/
  非正延迟属性组拒绝。

## Out of Scope

- stream() 竞速（137 显式不做）；多备对冲；对冲计数健康面（观测族后续）。

## Further Notes

- 至此韧性装配面族齐：重试/熔断（15）、降级链（15）、金丝雀（48）、shadow
  （49）、共享限流（54）、语义缓存（55）、共享熔断（57）、**对冲（本轮）**。
