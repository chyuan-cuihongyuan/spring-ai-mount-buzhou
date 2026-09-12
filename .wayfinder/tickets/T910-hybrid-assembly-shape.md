---
id: T910
title: 混合排序装配的形态裁决（含 SkillRanker 接口抽取）
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

HybridSkillRanker（spec 605）与 SemanticSkillRanker 无公共接口——SkillSearchTool/渲染器直依赖具体类，无法装配互换。怎么接？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 31 轮 = effort #600 / spec 630 / impl 483）：

1. 抽 `SkillRanker` 接口（rank 契约：稳定/降级原样）；Semantic/Hybrid 双实现；渲染器与检索工具参数面改接口（源兼容——既有传 Semantic 处自动适配）。
2. `buzhou.skills.hybrid-ranking.{enabled,lexical-weight}`（默认关零变化；启用需 EmbeddingModel 同语义档 fail-fast；hybrid 与 semantic 同开时 hybrid 胜——超集）。
3. 装配：HybridSkillRanker(semantic, lexical, 1.0, weight) 供目录渲染与 skill_search 共享（spec 73 同 ranker 纪律）。
