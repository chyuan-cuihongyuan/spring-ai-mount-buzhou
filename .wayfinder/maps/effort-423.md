# Wayfinder Map — Buzhou 错误偏向采样（effort #423，D 会话第 24 轮）

> D 会话第 24 轮（#407 扩散轮）。勘察：TurnSamplerHook（407）只在
> afterTurn 尾观察——而错误轮**不走 afterTurn**（DefaultAgentSession
> failTurnOnce 只调 observers.onTurnError，不 afterTurn）：错误轮在
> 采样面是盲区。低 rate 下最有评测价值的失败输入几乎全被均匀采样
> 淘汰。OTel tail_sampling 的 status_code=ERROR → always sample 规则
> 在本库缺失。

## Destination

`eval.TurnErrorSampler implements SessionObserver`（onTurnStart 记输入 →
onTurnError 采错；onTurnEnd 清场——成功路归 TurnSamplerHook）：

- 错误轮按 `error-rate-percent`（默认 **100**——错误轮全保）确定性采样
  入集（同 hash 公式 floorMod(sessionId:turn,100)——同轮同判）；
  response 占位 `[TURN-ERROR] 异常类: 消息(≤200 截断)`——golden 与否
  仍是人工判断（407 同口径）；
- `config.BuzhouErrorSamplingProperties`（buzhou.eval.error-sampling.
  {enabled, dataset, error-rate-percent, min-input-chars}）；
- 装配：EvalDatasetStore bean 条件放宽为「任一采样 enabled」（单 store
  不双 bean）；error-sampling RuntimeConfig 装配 customizer ctx.addObserver
  （per-session observer，sessionId 从装配 ctx 取——采样键确定性）。

## Notes

- 号段：spec 423 / T737–T738 / impl-396。
- 借鉴源：OpenTelemetry Collector tail_sampling（决策在 trace 终态——
  ERROR 全保、成功按基础率）+ 407 确定性 hash 同族。
- 纪律：fail-soft（采样是旁路绝不炸轮）+ 双计数；reactor 信号串行化
  保证 onTurnStart/onTurnError happens-before（普通字段安全，注记）。

## Out of scope

- 错误分类学采样（RetryCategory 分桶 rate）；成功/错误双率合并单一
  hook（缝不同——hook/observer 两面）；修复环中段错误捕获（只采终态）。

## Tickets

- [x] [T737 TurnErrorSampler 观察者](../tickets/T737-turn-error-sampler.md)
- [T738 yml 装配+E2E 错误轮入集](../tickets/T738-error-sampling-assembly.md)
