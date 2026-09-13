---
id: T1451
title: 工具策略匹配决策读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 1 轮：工具策略匹配决策读面（OPA decision log）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 1 轮 = effort #1000 / spec 1000 / impl 753）：缺口成立——四层覆盖模型解析单点 `ToolPolicyMatcher.match` 零观测：精确/通配/未命中不可分，策略配置退化（该精确配的都落在 `*` 兜底）无信号。落点 core.policy：新公共 record `ToolPolicyMatchDecision(toolName, Outcome(EXACT|GLOB|NONE), matchedKey)` + 快照 `ToolPolicyMatchStats(exactHits, globHits, noneHits, recent 有界环 32)`；match 判定单点同步累加，返回值逐位不变（现有调用方零感知）；`stats()` 读面 + `resetStats()` 测试隔离注入点（进程级静态态，BuzhouMetricsHolder 先例）。RunawayHook 复刻 glob 算法去重 out of scope（独立重构不混读面轮）。
