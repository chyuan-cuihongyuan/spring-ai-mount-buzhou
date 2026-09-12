---
id: T1003
title: 归档 PDB yml 装配的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

SessionAvailabilityFloor（spec 704）只有编程构造——yml 声明式入口缺失；且 liveSessions 全量计数在归档频率下有读放大。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 27 轮 = effort #726 / spec 726 / impl 529）：buzhouSessionArchiver bean 增 Environment + ObjectProvider\<SessionIndexStore\>——`buzhou.cleanup.min-available-sessions` >0 且索引在场时建 floor。**capped probe 计数**：liveSessions = `index.list(query(limit=min+1)).size()`——PDB 判定只需「>min 否」，一页 O(min) 读即答，不数全量。min=0 装配面不建 floor（零变化）；语义上 min=0 时 0 会话仍拒（保护最后一个会话——floor 语义自然延伸）。
