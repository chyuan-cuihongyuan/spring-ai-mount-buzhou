# Wayfinder Map — Buzhou LLM-as-judge 评估器（effort #21）

> effort #21（已闭合 2026-08-29），延续 #5–#20；收口后累计 168 轮 / impl 1–207。
> 主线：**LLM-as-judge 评估器**——推翻 effort #7「不内置」旧边界（当时 eval 面未成熟）；
> 今 spec 52 闭环 + 「可选模型 bean + 诚实边界」范式已建立（语义缓存/语义排序）。
> 借鉴 DeepEval / Ragas / LangSmith G-Eval：judge 模型按 rubric 打 PASS/FAIL。

## Destination

`LlmJudgeEvaluator implements Evaluator`（core.eval）：注入 judge ChatModel + 可选 rubric，
按「输出协议首词 PASS/FAIL」解析判定；runner 评估器异常收敛为该条 error（不再炸整跑）；
诚实边界（判别力归 judge 模型；CI 不强制；提示注入抗性归模型）入档；零新配置键。

## Notes

- 外部事实源：G-Eval（评分协议 prompt + 首词解析）；DeepEval GEval metric；Ragas
  llm-as-judge。本地裁定：PASS/FAIL 二值协议（不做 1-5 分映射——run 记录已是三态）。
- 本地勘察：`EvalRunner.runItem` 的 evaluate 异常会上抛炸整跑——顺带修复（error 三态）。

## Decisions so far

- 输出协议：judge 响应首词 PASS/FAIL（大小写/空白容差）；不可解析 → 该条 error。
- judge 异常 → error（runner 收敛）；rubric 默认语义等价判定，可注入自定义。
- prompt 组装经 Jackson 转义... 实际用纯文本分段（不拼 JSON）——注入面诚实入档。

## Not yet specified

- 成对对比（A/B judge）——下一 effort；judge 输出数值分与阈值。

## Out of scope

- 沿用 #7–#20；judge 模型供应链/温度控制（ChatModel 宿主责任）；新配置键。

## Tickets

- [x] [T273 LlmJudgeEvaluator + runner 评估器异常 error 三态](../tickets/T273-judge.md)（impl-207）
- [x] [T274 红队（协议解析/不可解析 error/rubric 注入/异常收敛/端到端 run）+ 文档面 + verify + 收口](../tickets/T274-judge-close.md)（impl-207；5 例 + eval 全回归 18 绿；累计 168 轮）
