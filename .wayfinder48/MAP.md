# Wayfinder Map — Buzhou Ragas 系数值评估器（effort #48，50 轮自迭代第 13 轮）

> effort #48，延续 #47（T331–T332 / impl-233）。主线：LlmJudgeEvaluator（spec 61）
> 只有二值 PASS/FAIL——「幻觉占比 25%」「相关性 9/10」这类连续质量信号表达不出；
> Ragas 的 faithfulness / answer-relevancy 是该面的成熟形态。

## Destination

`RagasEvaluators`：`faithfulness(judge[, threshold])`（断言被黄金答案支持率——
幻觉面）+ `answerRelevancy(judge[, threshold])`（对输入的针对性 0-10——跑题面）；
S x/y 数值协议（前缀容差 + clamp 0..1 + 越界/不可解析抛 JudgeProtocolException 走
error 收敛）；分母 0 从严 0.0；threshold 默认 0.8。与 EvalGate/eval loop 组合即成
连续质量门。

## Notes

- 借鉴：Ragas faithfulness（claim decomposition + NLI 的最小内核）/ answer
  relevancy。诚实边界：断言分解与判别归 judge 模型（spec 61 同口径）。

## Decisions so far

- 分母 0 → 0.0 从严（空输出不给满分——与 spec 80 error 入分母同精神）。
- 复用 JudgeProtocolException（协议家族同错误面，runner 收敛路径不变）。

## Not yet specified

- context precision/recall（需检索上下文字段——EvalItem 无此面，schema 另议）；
  G-Eval 自定义维度打分；claim 级明细（现在只有比例）。

## Out of scope

- 沿用 #7–#47；嵌入式向量化指标（语义相似度——embedding 面已有 ranker 另议）。

## Tickets

- [x] [T335 RagasEvaluators 双指标 + S x/y 协议](tickets/T335-ragas-evaluators.md)（impl-234）
- [x] [T336 4 例红队（协议/阈值双向/分母0/prompt 对照面）+ 收口](tickets/T336-ragas-close.md)
