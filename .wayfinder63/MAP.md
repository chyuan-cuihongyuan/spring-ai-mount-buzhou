# Wayfinder Map — Buzhou A/B 胜率门（effort #63，50 轮自迭代第 28 轮）

> effort #63，延续 #62（T373–T374 / impl-247）。主线：**spec 80 fog 项「A/B 门」**——
> EvalGate 覆盖单 run 质量水位，A/B 对比（spec 71/74）缺 CI 收口：换模型/换 prompt
> 后「B 不劣于 A」要宿主自己拼。

## Destination

`PairwiseGate.enforce(dataset, runtimeA, runtimeB, parallelism, minWinRateA)` →
`AbGateResult`（passed = winRateA ≥ threshold（clamp 0..1）+ 汇总 + 逐项预览截 10
条 [WINNER_A/WINNER_B/TIE/error] + CI 单行 summary）；error 项进预览不进分母
（spec 71 口径沿用——基础设施故障不是质量结论）。

## Notes

- 借鉴：Promptfoo model-compare gate / LiteLLM model-router 验收思想。

## Decisions so far

- 门 = A 胜率下限（B 视角换算：B 验收设 minWinRateA 低阈值）。

## Not yet specified

- 显著性检验（二项检验——统计面另议）；tie 的加权口径。

## Out of scope

- 沿用 #7–#62；exit-code 绑定（宿主 CI 语义）。

## Tickets

- [x] [T377 PairwiseGate 判定面](tickets/T377-ab-gate.md)（impl-248）
- [x] [T378 3 例红队（A 全胜/B 占多/error 不入分母）+ 收口](tickets/T378-ab-gate-close.md)
