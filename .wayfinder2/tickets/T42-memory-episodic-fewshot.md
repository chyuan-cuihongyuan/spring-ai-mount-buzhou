---
id: T42
title: memory · episodic memory few-shot
type: task
status: closed
assignee: ""
blocked-by: T28
created: 2026-08-14
---

## Question

成功经验如何复用为新任务的 few-shot 示例？事实源：LangGraph/LangChain（39,627★/144,172★：官方三类长期记忆 semantic/episodic/procedural；LangMem「episodic 将成功交互保存为学习示例」；实现面=BaseStore 层级命名空间+可选向量 IndexConfig——**「成功工具序列采集→注入」是文档层模式，本体只提供 Store 原语**）。

## 待定决策（研究推荐已备）

1. `EpisodeLedger{task_signature, goal, tool_trace_digest, outcome, embedding}`——采纳。
2. 采集：任务成功判定后的 hook（或 T37 sleep-time 整理器蒸馏）写入；检索：新任务以 goal 向量召回 top-k；注入：按预算渲染进 system prompt「过往成功示例」块（复用 T23 预算渲染机制）——采纳。
3. 成功信号判定质量是收益前提——**排在 T37/T38 之后**（研究 ROI 排序最后）——采纳。
4. procedural 产物对接规则知识库（.Knowledge）——本轮仅注记、不做。

依据：`docs/research/oss-perfect-tier23.md` §3.3（约 5 天，ROI 中）。

## Resolution

**Ratify 推荐 → [docs/spec/12-perfect-adoption.md](../../docs/spec/12-perfect-adoption.md) §memory-14**（用户常设授权 2026-08-14 ratify、可推翻）。EpisodeLedger 采集/goal 向量召回/预算注入 few-shot 块；排 Phase 6。
