---
id: T6105
title: S 会话 S3 Fisher-Yates 无偏洗牌的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

顺序随机化怎么 O(n) 无偏不引入排列偏差？（spec 5002 /
effort #5002 / S3）

## Resolution

**FisherYatesShuffle（core/policy）**：Durstenfeld 变体——从
尾向前每步 `nextInt(i+1)` 含自身均匀交换（无偏关键）；原地/
新列表/索引排列三变体，种子注入确定性；null/负 size
fail-fast。
