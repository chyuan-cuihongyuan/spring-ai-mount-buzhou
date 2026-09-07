# Wayfinder Map — Buzhou 幂等工具重试（effort #95，B 会话第 7 轮）

> B 会话第 7 轮。主题池「幂等工具重试」：外部工具瞬时故障（网络抖动/下游 503）
> 现在直接回喂错误反馈走 REASK——模型重述整轮，成本高且慢；幂等工具本可框架级
> 静默重试。借鉴 Temporal Activity retry policy + Failsafe 退避语义。

## Destination

RetryingToolCallback 装饰器（wrap 即启用，零 harness 改动）：可配 maxAttempts +
指数退避（封顶）+ 仅重试异常（错误反馈文案不重试——语义失败≠瞬时故障）；
幂等性契约归宿主声明（只包幂等工具）。

## Notes

- 号段：B=奇数 spec（本轮 133）。
- 装饰器而非 HarnessToolCallingManager 内嵌——共享热点文件零接触；与
  ToolCircuitBreakerHook 正交（熔断管「摘牌」，重试管「瞬时抖动」）。
- 重试次数计数可观测（buzhou.tool-retry.retries）。

## Decisions so far

- 异常才重试；返回错误文案（结构化标记）视为语义结局不重试。

## Not yet specified

- per-tool yml 配置面；重试事件外发。

## Out of scope

- 沿用 #7–#94；非幂等工具自动重试（不做也不谎称安全）；断路器联动。

## Tickets

- [x] [T479 RetryingToolCallback 装饰器（退避/上限/仅异常）](../tickets/T479-retrying-tool.md)（impl-279）
- [x] [T480 重试回归（抖动恢复/耗尽上抛/零变化/计数）](../tickets/T480-retrying-tool-tests.md)（impl-279）
