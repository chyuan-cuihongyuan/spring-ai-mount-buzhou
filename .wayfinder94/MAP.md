# Wayfinder Map — Buzhou 工具级熔断（effort #94，B 会话第 6 轮）

> B 会话第 6 轮。原拟「归档 autoconfig 定时 / 前缀缓存命中率」已被 A 会话收口
> （spec130 / spec126，A 提交 04f8566 明示「B 侧预告同主题请改选」）——本轮改选
> 主题池中的「工具级熔断」（未被占用）。

## Destination

per-tool 熔断器：失败率滑窗跳闸 → 冷却 → 半开探测恢复（resilience4j
CircuitBreaker 语义）；以 BuzhouHook 落地（beforeTool 拒 OPEN / afterTool 记
结局）——挂 hook 即启用，零挂零变化。

## Notes

- 号段：B=奇数 spec（本轮 131）；A=偶数 ≥130（双方提交声明）。
- 结局识别走 ToolFeedbackType 结构化标记（[工具执行失败]/[参数校验失败] = 失败）。
- 与 spec 15 模型熔断正交：那是模型面，这是工具面。

## Decisions so far

- hook 而非 HarnessToolCallingManager 内嵌——避免共享热点文件，且护栏语义
  （可拒绝的调用面）本就是 hook 的领域。

## Not yet specified

- 跳闸/恢复事件外发（webhook 家族）；熔断健康面段落。

## Out of scope

- 沿用 #7–#93；慢调用比例熔断（只有失败率面）；全局跨工具熔断。

## Tickets

- [x] [T477 ToolCircuitBreaker 状态机（滑窗/冷却/半开）](tickets/T477-tool-circuit-breaker.md)（impl-278）
- [x] [T478 ToolCircuitBreakerHook 接线 + 回归](tickets/T478-tool-breaker-hook-tests.md)（impl-278）
