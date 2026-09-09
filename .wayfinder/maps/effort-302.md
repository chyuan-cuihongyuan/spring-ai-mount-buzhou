# Wayfinder Map — Buzhou 重试预算接线（effort #302，C 会话第 3 轮）

> C 会话第 3 轮。spec 178 的 `RetryBudget`（Twitter Finagle retry budget）是
> standalone 原语——预算类与单测俱全，但模型重试（ResilienceAdvisor）与工具
> 重试（RetryingToolCallback）都不咨询它：上游故障时重试量无进程级上限，
> 重试风暴风险（fog 152「RetryBudget 接线」项）。

## Destination

进程级重试预算接入两条重试路径：模型重试前 `tryAcquire`（拒即原错上抛，
retry-budget-denied 可观测）、工具重试前同口径；每次逻辑调用 `deposit`
（流量即预算）；yml `buzhou.backpressure.retry-budget.*` 声明即全局生效，
未配置零行为变化。

## Notes

- 借鉴：Twitter Finagle retry budget（spec 178 原始来源——本轮装配收尾）。
- 号段：本轮 spec 302 / T595–T596 / impl-325。

## Decisions so far

- Holder 模式（RetryBudgetHolder，进程单例静态，ToolResultLimiterHolder 同型）
  ——advisor/callback 动态读取，零构造器签名变更。
- yml 装配：`buzhou.backpressure.retry-budget`（percent 默认 20、min-balance
  默认 10；两者全未配置 = 关）；autoconfig bean 设置 holder、容器关闭清理
  （防 ApplicationContextRunner 跨上下文静态污染）。
- 拒绝语义：不重试、原异常上抛（与重试耗尽同词汇路径），事件/计数双观测面。

## Not yet specified

- 多实例共享预算后端（归 Redis 共享族 #339 一并考量）。

## Out of scope

- Turn 内 REASK 次数（TurnLoopPolicy.retryBudget 是另一概念，不混）；
- per-session 预算（预算的要点恰是进程级流量占比）。

## Tickets

- [x] [T595 RetryBudgetHolder + 模型/工具两路径 tryAcquire/deposit](../tickets/T595-retry-budget-wiring.md)（impl-325）
- [x] [T596 yml 装配 + 拒绝语义三面回归](../tickets/T596-retry-budget-close.md)（impl-325）
