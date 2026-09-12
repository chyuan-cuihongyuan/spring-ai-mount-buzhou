---
id: T1066
title: 提示词使用缺口读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
零使用提示词与统计孤儿无读数——加联合差集吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 34 轮 = effort #733 / spec 733 / impl 633）：`PromptUsageGaps.analyze(declaredNames, rows)` 纯函数——unused（声明∩零使用字典序）+orphans（stats 有而 registry 无）。纯读数不改双方。
