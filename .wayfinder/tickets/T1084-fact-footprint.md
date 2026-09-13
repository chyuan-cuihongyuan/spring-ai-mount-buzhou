---
id: T1084
title: 共享事实足迹读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
事实库 owner 维度无归因——加足迹读数吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 42 轮 = effort #741 / spec 741 / impl 641）：`SharedFactFootprint.analyze(List<SharedFact>)` 纯函数——rows(owner, facts, eternal) facts 降序+字典序稳定+totalFacts/eternalFacts。值不读取（隐私口径）。纯读数。
