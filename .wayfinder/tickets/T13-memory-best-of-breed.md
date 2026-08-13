---
id: T13
title: memory 对标开源最优——Mem0 对账/语义触发/预算渲染 等 best-of-breed 落地
type: task
status: open
assignee: ""
blocked-by: [T11]
created: 2026-08-13
---

## Question

把 [T11](T11-oss-best-ideas-core-memory-spill-guard.md) / `docs/research/oss-best-of-breed.md` §2 §5 里 **memory** 的 best-of-breed 思想择优写入。T3 闭合于「SPEC 判据满足」；本票抬杆到「对标开源最优」。落地项：

### Tier 1（高价值，先做）
- **ADD/UPDATE/DELETE/NOOP 事实对账**（Mem0）：9 段摘要每段生成后跑对账 pass（embedding 近邻 + LLM 裁决），防重复/矛盾/陈旧——最强去重/幂等。
- **语义边界触发**（LangChain Deep Agents）：给 LLM `compact_now` 工具在任务边界自触发；token 阈值仅作安全网（双触发路径）。
- **双时序事实有效性**（Zep/Graphiti）：事实被取代时标 `valid_until` 而非删，支持时序查询、避免断崖丢旧。
- **增量摘要**（LangMem `RunningSummary`：`summarized_message_ids`）：只把新消息折入既有摘要，避全量重摘要漂移。
- **把预算拆解渲染给 LLM**（Letta `chars_current/chars_limit`）：9 段每段加预算页脚，让 LLM 自削 P3。

### Tier 2（更重）
- memory-as-tools：暴露 `search_evidence(evidence_id)`（Buzhou 已有 evidence-id！）+ `revise_summary_section`，让 agent 自愈压缩错误（Letta）。
- 向量 recall 三模搜（timestamp/text/embedding）回查精确原文（MemGPT recall）。
- episodic memory 作 few-shot（LangGraph）。
- sleep-time 后台整理 9 段（Letta）。

### Tier 3（廉价 wins）
- 每轮 evict ~70%（非 100%）保连续（Letta）。
- 压缩前存检查点可回滚（Cline/Claude Code）。
- 压缩保真度 eval（trace 注入 follow-up，Deep Agents）。

## Context

- **已领先（勿重做）**：微压缩 evidence-id（**业界唯一确定性回读指针**）、9 段 P0..P3 段内分级（领先多数）、动态预算（领先 Spring AI/Vercel/ADK）。
- 反模式：纯滑窗丢最旧；仅静态 85% 阈值；非结构化单块摘要；规则截断回退；矛盾即删；全量重摘要；100% 才触发。
- 详源见 `docs/research/oss-best-of-breed.md` §2 与 §6（MemGPT/Letta / LangGraph / LangMem / Deep Agents / LlamaIndex / Zep / Mem0 / Claude Code / Cline）。

## Resolution
<!-- 实现后填 -->

## Resolution（Tier-1 部分，2026-08-13）

Tier-1 五项全部落地并闭合于细化票：[预算渲染](T23-memory-budget-render-to-model.md)、[增量摘要](T24-memory-incremental-summary.md)、[Mem0 对账](T25-memory-mem0-fact-reconciliation.md)、[双时序有效性](T26-memory-bi-temporal-fact-validity.md)、[compact_now 语义触发](T27-memory-semantic-boundary-compact-trigger.md)。Tier-2（memory-as-tools/向量 recall/episodic/sleep-time）与 Tier-3 继续由本票追踪。
