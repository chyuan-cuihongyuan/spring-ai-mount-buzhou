# 1621 · 会话隔离检疫装配（spec 143 双孤类救活）

> 来源：N 会话 R22（effort #1621 / T2393–T2394 / impl 1174）。spec 1611 普查修复
> 第八弹：SessionQuarantine + SessionQuarantineHook（spec 143）互引成孤岛——
> 无任何 Module/AutoConfiguration 注册。

## Solution

- 装配 bean（BuzhouCoreAutoConfiguration）：`buzhou.quarantine.enabled=true`
  opt-in（默认关——检疫 block 轮次，行为面大须显式开启）；`failure-threshold`
  / `base-backoff` / `max-backoff` 可配（缺省 3 / 30s / 10m，双格式时长解析）。
  BuzhouHook bean 经 List<BuzhouHook> 自动收集；SessionQuarantine 进程级。
- 语义照原设计：beforeTurn（order 40）隔离中 block 可读理由；onModelError 计败；
  成功复位走 `recordTurnSuccess` 公共 API（hook 面看不到健康轮全貌——不谎装）。

## Testing Decisions

- `SessionQuarantineHookTest` 两断言：三连败 → beforeTurn Block + snapshot
  remaining 正 + 冷却过后自动解除放行；健康会话零状态（snapshot 无条目——诚实口径）。
- 回归：SessionQuarantineTest 6 用例。

## Out of Scope

- 成功复位的自动接线（afterTurn 语义不完整——原设计已声明诚实边界）。
- 隔离事件流（tripped counter 已有指标）。
