---
id: T30
title: core · 工具参数 schema 校验 + per-turn 重试预算
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

工具参数不过 schema 时，当前会怎样？应否在**执行前**校验并以有限预算回喂模型重试？事实源：Pydantic AI（19,271★：`ValidationError` 自动转 retry 消息、`ModelRetry` vs `ToolFailed` 两档词汇、**默认 retries=1**）、Instructor（13,726★：`max_retries` + REASK）。

## 待定决策（研究推荐已备）

1. 工具执行前对 arguments 做 JSON Schema 校验（复用 spring-ai 工具 schema 生成），失败构造 **`ToolValidationFeedback`**（区别于执行期 `ToolErrorFeedback`，两档词汇分明）——采纳。
2. `TurnLoopPolicy` 增 **`retryBudget`**（per-turn 计数器，默认 1–2，与 Turn 上界独立扣减），耗尽转 REASK_FAILED 停止条件——采纳。
3. 校验错误文案形状（错误 + 原 args + 修正提示，对齐 T16 错误回喂格式）——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §2.4（2–3 天，ROI 高，纯增量扩展现有两机制）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §core-2**（用户常设授权 2026-08-14 ratify、可推翻）。执行前 schema 校验→ToolValidationFeedback；TurnLoopPolicy.retryBudget 默认 1–2、耗尽转 REASK_FAILED。
