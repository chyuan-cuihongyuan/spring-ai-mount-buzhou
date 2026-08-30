# Spec 71 — A/B 成对评估 runner（effort #31）

> wayfinder map：`.wayfinder31/MAP.md`（T293–T294）。OSS 借鉴：Ragas pairwise eval /
> LiteLLM model-compare。组合 #21（judge）/ #23（pairwise）/ #28（并行）三件。

## Problem Statement

模型/配置 A 与 B 的对比需要人工拼装：跑两遍数据集、逐项配对、调 judge、手算胜率——
没有一体化面。

## Solution

`PairwiseEvalRunner.compare(dataset, runtimeA, runtimeB, parallelism)`：逐项双路隔离执行
+ PairwiseJudge 双向裁定（位置偏差消解内置）+ 汇总（winsA/winsB/ties/errors/胜率——
error 不入分母）；项序确定；并行 clamp 1..32。

## User Stories

1. 作为评估作者，我要一键 A/B 对比，所以模型选型/配置变更有量化依据。
2. 作为红队，我要单路执行异常记 error 不裁胜负，所以胜率不被基础设施故障污染。
3. 作为红队，我要并行与串行等值同序，所以确定性保持。

## Implementation Decisions

- 复用三件套；A/B 会话 id 带侧标记（观测可区分）；API 返回面（暂不落盘 run 记录——
  fog 留位）。

## Testing Decisions

- 全胜胜率 1.0；单路异常 error；并行/串行等值同序（内容型 stub judge）。

## Out of Scope

- >2 路对比；run 落盘与事件；新配置键。

## Further Notes

- 判别力/抗注入归 judge 模型（#21 同口径）。
