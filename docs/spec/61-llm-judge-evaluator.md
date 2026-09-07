# Spec 61 — LLM-as-judge 评估器（effort #21）

> wayfinder map：`.wayfinder/maps/effort-21.md`（T273–T274）。OSS 借鉴：LangSmith G-Eval /
> DeepEval / Ragas 的 judge 评分协议。

## Problem Statement

内置评估器只有确定性三件（EXACT/CONTAINS/REGEX）；语义质量类断言（「回答是否解决了
用户问题」「是否与事实一致」）无法用三者表达——宿主只能各自手写 judge 逻辑，重复且
口径不一。effort #7 时「不内置」的边界前提（eval 面未成熟）已被 spec 52 闭环推翻。

## Solution

内置 `LlmJudgeEvaluator`：注入 judge ChatModel 与可选 rubric，按输出协议（响应首词
PASS/FAIL，大小写/空白容差）判定；不可解析或 judge 异常收敛为该条 error（runner 不再
因单个评估项异常炸整跑）。诚实边界：判别力与抗提示注入归 judge 模型；CI 不强制
（runbook §9 已有 LLM-judge + 人工抽检口径）；无温度控制（ChatModel 宿主责任）。

## User Stories

1. 作为评估作者，我要语义质量断言，所以能评估「回答是否解决问题」而非仅字面匹配。
2. 作为评估作者，我要自定义 rubric，所以领域口径（合规/语气/精度）可插拔。
3. 作为运维者，我要 judge 异常记为该条 error 而非炸整跑，所以一次 API 抖动不废一次评估。
4. 作为红队，我要协议解析容差（大小写/空白/前缀文案）被钉住，所以解析不回退。
5. 作为红队，我要不可解析输出记 error（不猜成 FAIL），所以 passRate 不被协议失败污染。
6. 作为既有用户，我要确定性评估器行为零变化，所以升级零风险。

## Implementation Decisions

- `LlmJudgeEvaluator(ChatModel judge)` / `(ChatModel judge, String rubric)`；
  默认 rubric = 语义等价判定；prompt = 系统段（协议 + rubric）+ 用户段（期望/实际
  分隔线隔开，不拼 JSON）。
- 解析：响应 trim 后首词（忽略大小写）PASS/FAIL；其余 → 抛协议异常（runner 记 error）。
- runner：`evaluator.evaluate` 包 try/catch → `STATUS_ERROR`（"评估器异常：…"），
  与执行异常同三态语义。
- 零新配置键；不注册进 BuiltInEvaluators（需 ChatModel，宿主显式构造传入 run）。

## Testing Decisions

- stub judge（ScriptedChatModel）：PASS/FAIL/大小写/前缀文案/不可解析/异常收敛；
  端到端 run 一条 passRate 汇总正确。
- 先例：SemanticRankingTest 的 stub ChatModel 形态沿用。

## Out of Scope

- 成对对比评估（下一 effort）；数值分与阈值；温度控制；新配置键。

## Further Notes

- judge 抗提示注入归模型（诚实入档）；rubric 拼接不做消毒——评估面信任边界内。
