# Wayfinder Map — Buzhou A/B 成对评估 runner（effort #31）

> effort #31，延续 #5–#30（累计 177 轮 / T1–T292 / impl 1–216）。
> 主线：**A/B 评估闭环**——组合作品：EvalRunner 项执行 + PairwiseJudge 双向裁决 +
> 虚拟线程并行 → 同数据集双 runtime 对比跑（胜率汇总）。借鉴 Ragas pairwise eval /
> LiteLLM model-compare。

## Destination

`PairwiseEvalRunner.compare(dataset, runtimeA, runtimeB, parallelism)`：逐项双路执行
（异常 → 该项 error 不裁胜负）+ 双向裁定 + 汇总（winsA/winsB/ties/errors + 胜率，
error 不入分母）；项序确定；零新键。

## Notes

- 组合面复用 #21/#23/#28 三件；诚实边界同口径（判别力归 judge）。

## Decisions so far

- 胜率分母 = wins+ties+losses（error 诚实分离）；A/B 会话隔离 id 带侧标记。

## Not yet specified

- run 记录落盘（state store）与事件（需求证据后议——当前 API 返回面已够）。

## Out of scope

- 沿用 #7–#30；多路（>2）对比；新配置键。

## Tickets

- [x] [T293 PairwiseEvalRunner（双路执行 + 裁决 + 汇总）](../tickets/T293-ab-runner.md)（impl-217）
- [x] [T294 红队（全胜胜率/单路 error/并行等值）+ 文档 + verify + 收口](../tickets/T294-ab-close.md)
