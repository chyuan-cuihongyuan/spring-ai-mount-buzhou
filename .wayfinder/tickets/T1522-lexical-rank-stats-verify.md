---
id: T1522
title: 词法排序生效计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1521
created: 2026-09-14
---

## Question

J 会话第 35 轮：排序生效计数如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（LexicalRankStatsTest，复用 ClasspathSkillScanner 骨架）：问法命中（code 相关）→ runs=1 且 reordered=1（顺序确实变化）；空问法/单候选早返不计 runs；reordered 与 runs 对账（reordered ≤ runs）。定向 `mvn -pl buzhou-skills test -Dtest='LexicalRankStatsTest,LexicalSkillRankerTest'` 绿（后者若存在）。
