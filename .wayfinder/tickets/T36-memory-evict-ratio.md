---
id: T36
title: memory · evictRatio 部分逐出保连续
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

微压缩/摘要逐出消息时，比例如何定才能保上下文连续？事实源：Letta（24,230★：博客「evict only a portion (e.g., 70%)」；SDK `sliding_window_percentage` **默认 0.3**（摘要 30%/保留 70%）、不够按 ~10% 步进升级、`clip_chars` 上限 50,000）；旁证 Cline 66,136★（trigger 0.9/target 0.7）、LangChain（保 10% 近期）。

## 待定决策（研究推荐已备）

1. `evictRatio` 参数化（**默认 0.7**：逐出约 70% 候选、保留 30% 原文续接）+ **10% 步进升级梯子**（预算仍超时逐级加压）——采纳。
2. 不变式：**最近 N turn 原文 + 上一次增量摘要永不逐出**（与 `summarized_message_ids` 双水位兼容）——采纳。
3. 与既有 P0–P3 优先级的交互（P3 段先承受逐出压力）——spec 定。

依据：`docs/research/oss-perfect-tier23.md` §3.5（1–2 天，ROI 高，全 effort 最便宜）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §memory-8**（用户常设授权 2026-08-14 ratify、可推翻）。evictRatio 默认 0.7+10% 步进梯子；最近 N Turn 原文+上次增量摘要永不逐出。
