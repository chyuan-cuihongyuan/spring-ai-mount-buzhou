---
id: T1521
title: 词法排序生效计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 35 轮：词法排序生效计数读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 35 轮 = effort #1034 / spec 1034 / impl 787）：缺口成立——LexicalSkillRanker（spec 605 BM25 词法排序，opt-in）rank 全程零计数：排序跑了多少次、**实际改变顺序**多少次不可见——「排序器是否空转」（问法与描述词法永不相交=配置错位）无信号。落点 buzhou-skill：实例级 runs（有效 BM25 运行数：候选>1 且问法非空）/reordered（输出序 ≠ 输入序）两 AtomicLong + 嵌套 record `RankStats(runs, reordered)` + `stats()`。实例级；嵌套类型不动 API 快照；排序返回值逐位不变。
