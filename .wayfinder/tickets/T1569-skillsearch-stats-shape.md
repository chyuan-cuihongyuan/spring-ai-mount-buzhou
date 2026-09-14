---
id: T1569
title: skill_search 搜索判定读面（SkillSearchStats）的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1567
created: 2026-09-15
---

## Question

J 会话第 57 轮：skills 域的读面增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题（跨模块轮换）：SkillSearchTool（模型搜索可用技能的内置工具）全路径零计数——解析失败、空 query、命中、零结果全部静默。零结果率是搜索质量第一信号（Algolia/Elasticsearch zero-result-rate 思想）：模型反复搜不到技能说明可见性绑定或描述质量有问题。

形状裁决：`SkillSearchTool` 内静态 `AtomicLong` 五计数——calls（入口）/ hits（匹配非空）/ misses（零结果，含语义建议分支触发场景）/ parseRejects（解析失败）/ blankQueryRejects（空 query）；嵌套 `record SkillSearchStats` + `stats()` + `resetForTest()`。守恒 `calls = hits + misses + parseRejects + blankQueryRejects`。静态面理由同族先例；call() 返回语义逐位不变。

Out of scope：按 query 文本分桶（入参敏感面）；语义建议采纳率（模型后续行为，另轴）。
