---
id: T39
title: memory · 压缩保真度 eval
type: task
status: closed
assignee: ""
blocked-by: T29
created: 2026-08-14
---

## Question

九段式摘要的质量如何持续护栏？事实源：LangChain（144,172★：Deep Agents 85% 触发保 10% 近期；eval 方法论=自有 traces「向应压缩与不应压缩的线程注入 follow-up prompts」构造正负用例验证触发时机 + dogfooding）。**诚实声明**：「断言 follow-up 答案压缩后仍可答」的保真断言模式是研究子 agent 基于 trace 注入框架的合理延伸，标注「建议自建」。

## 待定决策（研究推荐已备）

1. `CompactionFidelityEval`：回放录制会话（**依赖 T29 record/replay 基建**）→ 注入仅在压缩前水位之下可答的 follow-up → 用压缩后上下文跑 agent → LLM judge + **evidence-id 精确断言**答案保住——采纳。
2. 负例集（不应压缩场景）防误触发；指标=保真率/误触发率/压缩比——采纳。
3. 评测式断言沿用既有 `SummaryEvaluationTest` 方法论；是否上 CI 门禁（prompt 变更时跑）——spec 定。
4. judge 模型选型（gated 真实 LLM 或脚本化 stub 双轨）——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §3.7（5–8 天，ROI 高：九段式摘要的质量护栏）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §memory-11**（用户常设授权 2026-08-14 ratify、可推翻）。record/replay 回放+follow-up 注入+LLM judge+evidence-id 断言+误触发负例集。
