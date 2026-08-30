# Spec 87 — Ragas 系数值评估器（effort #48）

> wayfinder map：`.wayfinder48/MAP.md`（T335–T336）。借鉴：Ragas faithfulness /
> answer-relevancy。

## Problem Statement

LlmJudgeEvaluator（spec 61）是二值 PASS/FAIL：「幻觉占比 25%」「相关性 9/10」这类
连续质量信号表达不出——阈值调优、趋势对比、质量门分级都需要连续分。

## Solution

`RagasEvaluators`（eval 包）：
- `faithfulness(judge[, threshold=0.8])`——judge 分解实际输出为事实断言、判每条
  是否被期望输出（黄金答案）支持；score = 支持/总断言（幻觉面）。
- `answerRelevancy(judge[, threshold=0.8])`——judge 对「实际输出回应输入（用户
  问题）」打 0-10；score = n/10（跑题面）。
- 协议：judge 回复 `S x/y`（前缀空白容差）；clamp 0..1；`x > y` 或不可解析抛
  `JudgeProtocolException`（runner 收敛该条 error——与 spec 61 同口径）；分母 0
  → 0.0 从严（detail 标注）。

## User Stories

1. 作为评估作者，我要连续分指标，所以阈值可调、趋势可比。
2. 作为质量负责人，我要幻觉率可量化，所以编造型回归可追踪。
3. 作为红队，我要协议失败走 error 不猜分，所以分值不被协议噪声污染。

## Implementation Decisions

- 两指标共用 ScoredJudge 内核（协议/钳制/异常同面，rubric 差异化）。
- faithfulness 对照 expected、relevancy 对照 input（各自正确的参照系——测试钉住）。

## Testing Decisions

- 协议解析（前缀容差）+ 阈值双向（0.75 过 0.7 不过 0.8）；relevancy 9/10 过 3/10
  不过；分母 0 从严 + detail 标注；协议失败/越界异常；prompt 对照面（faithfulness
  含 expected 无「用户输入」、relevancy 含 input）。

## Out of Scope

- context precision/recall（EvalItem 无上下文字段）；G-Eval 维度打分；claim 明细。

## Further Notes

- 与 EvalGate 组合 = 连续质量门；与 EvalRunDiff 组合 = 质量趋势回归。
