# 1506 — A/B 对比 run 宿主取消（spec 1505 扩散）

> 来源：M 会话第 7 轮 = effort #1506（impl 1109）。spec 1505 取消语义在 PairwiseEvalRunner 域的同源扩散（仓库「NNN 扩散」先例模式）。

## 背景

A/B 对比 run 已有 SPRT 序贯提前终止（spec 1605，统计达界自动停）与 skipped 桶，但无宿主主动叫停通道——对比跑双 runtime 成本翻倍，配错方向时止损价值更高。

## 目标

- `requestCancel()`：实例级标记；compare 开始清零；串行/并行两路径未起项检查（与 earlyStop 同位，复用 skipped 桶）；
- `PairwiseSummary` 加 `hostCancelled` 布尔（区分统计达界停与宿主叫停）；9/7 参兼容构造器保留；
- 序列化仅取消 run 落 `hostCancelled` 位（缺省省位）；指标 `buzhou.eval.ab.cancelled`。

## 兼容性

纯新增；未取消 run hostCancelled 恒 false 零行为变化。
