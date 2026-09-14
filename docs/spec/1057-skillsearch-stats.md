# 1057 — skill_search 搜索判定读面

> 来源：J 会话第 57 轮 = effort #1057（[T1569](../../.wayfinder/tickets/T1569-skillsearch-stats-shape.md) / [T1570](../../.wayfinder/tickets/T1570-skillsearch-stats-verify.md) / impl 809）。借鉴：Algolia/Elasticsearch zero-result-rate（零结果查询率是搜索质量与可见性配置的第一信号）。J 会话首个 skills 域轮。

## Problem Statement

`SkillSearchTool`（模型按关键词检索可用技能的内置工具）全路径零计数——解析失败、空 query、命中、零结果（含语义建议兜底）全部静默返回：**搜索质量与技能可见性健康不可见**。宿主无法回答「模型搜索命中率多少、零结果集中在哪」——零结果高发意味着可见性绑定错配或技能描述质量差，模型在盲选工具。

## 目标

- `SkillSearchTool` 增量（skill，静态面）：五 `AtomicLong`。
  - `calls`：call 入口（总桶）；`hits`：匹配集合非空；`misses`：零结果（语义建议分支属 misses 场景下的兜底，不改桶）；`parseRejects`（解析失败）/ `blankQueryRejects`（空 query）。
- 嵌套 `record SkillSearchStats(long calls, long hits, long misses, long parseRejects, long blankQueryRejects)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**calls = hits + misses + parseRejects + blankQueryRejects**（每入口恰落一桶）。

## 兼容性

纯增量读面：call() 返回语义、可见性过滤与关键词/语义建议匹配逐位不变；静态面理由同 R46–R56 先例；无新配置项。

## Out of Scope

- 按 query 文本分桶（入参敏感面——红线纪律）。
- 语义建议采纳率（模型后续行为，另轴）。
