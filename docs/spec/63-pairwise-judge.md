# Spec 63 — 成对对比评估（effort #23）

> wayfinder map：`.wayfinder/maps/effort-23.md`（T277–T278）。OSS 借鉴：Ragas pairwise /
> Chatbot Arena 的双向评判消位置偏差。

## Problem Statement

LlmJudgeEvaluator 只能单输出判定；「两个版本/模型谁更好」的成对问题需要不同形态——
且 LLM judge 有实证的首位展示偏好（position bias）：同一对输出交换顺序后判定翻转。

## Solution

`PairwiseJudge`：双向评判（(A,B) 与 (B,A) 各一次）——两方向裁定同一赢家才判
WINNER_A/WINNER_B；不一致判 TIE 并标注 position-bias（偏差显性化而非静默）；协议
解析失败判 TIE 并标注 protocol。返回 `PairwiseVerdict(winner, reason)`。

## User Stories

1. 作为评估作者，我要成对比较两个输出，所以能评估版本/模型相对质量。
2. 作为红队，我要位置偏差被双向消解（首尾偏好判 TIE），所以 judge 偏差不冒充裁决。
3. 作为红队，我要协议失败标注为 protocol-TIE 而非猜测，所以失败可区分于真平局。
4. 作为既有用户，我要零行为变化，所以升级零风险。

## Implementation Decisions

- 词汇表：judge 响应首词 WINNER_A / WINNER_B / TIE（双向各自解析）。
- 一致裁决映射：(A,B)→WINNER_A 且 (B,A)→WINNER_B 才裁 A（对称裁 B）；其余组合 TIE。
- 复用 #21 协议形态；rubric 可注入（默认「哪个输出更好地回应输入」）。

## Testing Decisions

- stub judge 按内容标记裁决（与位置无关）→ 赢家；按位置裁决（恒「第一个」）→ TIE；
  协议失败 → protocol TIE；双向 prompt 断言（两次调用顺序相反）。

## Out of Scope

- A/B 运行编排（宿主驱动）；数值分；新配置键。

## Further Notes

- 判别力/抗注入归 judge 模型（#21 同口径）；成本 = 每对 2 次 judge 调用。
