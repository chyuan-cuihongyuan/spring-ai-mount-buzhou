# Spec 101 — A/B 胜率门（effort #63）

> wayfinder map：`.wayfinder63/MAP.md`（T377–T378）。spec 80 fog 项收口。
> 借鉴：Promptfoo model-compare gate / LiteLLM model-router 验收。

## Problem Statement

A/B 对比（spec 71/74）有 runner、落盘、事件、导出，但缺 CI 收口：换模型/换 prompt
后「B 不劣于 A」的判定要宿主自己拼——每家口径漂移（tie 怎么算、error 入不入分母）。

## Solution

`PairwiseGate.enforce(dataset, runtimeA, runtimeB, parallelism, minWinRateA)` →
`AbGateResult`：passed = winRateA ≥ threshold（clamp 0..1；B 验收视角设低阈值）；
汇总（winRateA/B + winsA/B + ties + errors/total）+ 逐项预览（[WINNER_A/WINNER_B/
TIE/error] 截 10 条）+ CI 单行 `summary()`。error 项进预览不进分母（spec 71 口径
沿用）。执行复用 PairwiseEvalRunner 管线（落盘/事件/注册表不重复）。

## User Stories

1. 作为 CI 作者，我要换版验收一行判定，所以 B 回归在流水线即红。
2. 作为质量负责人，我要 error 与质量诚实分离，所以基础设施故障不伪装成质量结论。

## Implementation Decisions

- 门 = A 胜率下限（单指标——显著性检验 fog 记账不预设）。

## Testing Decisions

- A 全胜过 0.8 门 + OK 摘要；B 全胜不过 + 预览全 WINNER_B + 0.4 档方向性；
  B 路全挂 error：分母 0 → winRate 0 + clamp 0 边界过 + 预览全 [error]。

## Out of Scope

- 显著性检验；tie 加权；exit-code 绑定。

## Further Notes

- 与 EvalGate（spec 80）组成双门族：质量水位门 + 相对胜率门。
