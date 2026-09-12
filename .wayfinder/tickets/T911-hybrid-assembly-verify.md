---
id: T911
title: 混合排序装配验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T910
created: 2026-09-12
---

## Question

装配语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（HybridRankingAssemblyTest 3/3 + skills 全模块 104/104 零回归 + 快照随轮再生 SkillRanker）：

- hybrid=true 无 EmbeddingModel：fail-fast 带 hybrid-ranking 修法提示。
- 缺省零变化（无排序器）。
- yml 键（enabled/lexical-weight）解析装配成功。
