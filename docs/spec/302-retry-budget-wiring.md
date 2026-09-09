# Spec 302 — 重试预算接线（effort #302）

> wayfinder map：`.wayfinder/maps/effort-302.md`（T595–T596）。借鉴：Twitter Finagle
> retry budget（spec 178 原语装配收尾——fog 152「RetryBudget 接线」项）。

## Problem Statement

模型重试（ResilienceAdvisor 指数退避）与工具重试（RetryingToolCallback）各自
为政：上游故障时 N 并发会话 × 每会话 maxAttempts 次重试叠加放大流量——重试
风暴无进程级上限。`RetryBudget`（spec 178）已建但两条路径都不咨询它。

## Solution

进程级重试预算接入两条重试路径（未配置 = 零行为变化）：

- 每次<b>逻辑调用</b>（模型 adviseCall 进入 / 工具 call 进入）`deposit`——
  流量即预算来源（Finagle 语义）。
- 每次<b>重试发起前</b> `tryAcquire`：余额不足 → 不重试、原异常上抛（与重试
  耗尽同词汇路径），模型侧发 retry-exhausted 事件（attempts 口径如实）+
  `buzhou.resilience.retry-budget-denied` 计数；工具侧直接抛最后异常。
- 载体：`RetryBudgetHolder`（进程单例静态，ToolResultLimiterHolder 同型）；
  `RetryingToolCallback.wrap(delegate, policy)` 默认取 holder 当前值，
  三参 wrap 可显式传入。
- yml：`buzhou.backpressure.retry-budget.percent`（默认 20）/`min-balance`
  （默认 10）；两者全未配置 = 关。autoconfig 装配 bean 设置 holder，容器
  关闭时清理。

## User Stories

1. 作为运维，上游半故障时重试量自动被压到流量占比（percent）内——无需人工
   熔断重试开关。
2. 作为运维，`denied()` 计数与 retry-budget-denied 事件即「风暴被压制的
   证据面」。
3. 作为开发者，未配置预算时两条路径行为逐位不变（opt-in 诚实边界）。

## Implementation Decisions

- Holder 静态单例（进程级语义本意），不做 per-session。
- 拒绝即终态（不 sleep 不重试）——预算的意义就是少打一枪。

## Testing Decisions

- core `RetryBudgetWiringTest`：工具重试预算拒绝中途 fail-fast（最后异常
  上抛、无多余尝试）；每次调用 deposit；holder null 时重试照常。
- core `RetryBudgetAssemblyTest`：yml 两键装配 holder 生效；未配置不设；
  容器关闭清理（ApplicationContextRunner 静态隔离）。
- resilience `RetryBudgetAdvisorTest`（Buzhou.runtime 端到端，对齐
  ResilienceEndToEndTest 快退避模板）：零余额首失败即拒绝——单次尝试、
  原错误上抛、denied 计数 1；有余额时重试成功路径不受影响。

## Out of Scope

- 多实例共享预算后端（Redis 族）；per-session 预算；Turn REASK 次数（异概念）。

## Further Notes

- 背压族装配状态：spawn 闸（14A wired）/ 工具扇出闸（wired）/ 限流（15
  wired）/ 弹性预算池（157 wired）/ **重试预算（178 standalone→本轮 wired）**。
