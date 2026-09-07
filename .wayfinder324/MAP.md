# Wayfinder Map — Buzhou 工具金丝雀（effort #324，C 会话第 25 轮）

> C 会话第 25 轮。shadow 对照（309）是"旁路对比不上流量"；真上流量的新
> 工具版本缺一个安全档：按权重分流 + 出错率劣化自动回滚——Istio/Flagger
> canary 的库内等价物（装饰器族先例 169 TransformingToolCallback）。

## Destination

`CanaryToolCallback`（exec 装饰器）：同一定义的两实现按 weight% 分流；
per 臂成败计数（131 同标记语义）；canary 样本足且错误率劣化超容差 →
**一次性自动回滚**（粘性——不再自动回升，人工重开）；观测 View。
工厂 `wrap(stable, canary, weightPercent, random)`。

## Notes

- 号段：spec 324 / T639–T640 / impl-347。
- 借鉴：Istio 权重分流 + Flagger canary 分析回滚。

## Decisions so far

- 装饰器族无 yml（宿主构造两实现后 wrap——169 先例）。
- 两臂 ToolDefinition 同名校验（同名工具才可灰度，否则 IAE）。
- 计数累计制（无时间窗）——窗口版按需；回滚一次性粘性。

## Out of scope

- 按时间窗的错误率对比；指标联动自动 promote（只自动回滚不自动晋升——
  晋升是人的决定）；多臂（>2 版本）。

## Tickets

- [x] [T639 CanaryToolCallback 本体](tickets/T639-canary-callback.md)（impl-347）
- [x] [T640 回归与收口](tickets/T640-canary-close.md)（impl-347）
