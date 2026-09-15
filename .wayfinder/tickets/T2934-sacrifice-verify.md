---
id: T2934
title: 缓存牺牲率的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2933]
created: 2026-09-16
---

## Question]

牺牲面在理想/颠簸双条件/哨兵无判/畸形四面下正确吗？（spec 1866 / effort #1866 / R67）

## Resolution`

**CacheSacrificeRatioTest 4 用例全绿**（mvn -pl buzhou-resilience test
-Dtest=CacheSacrificeRatioTest）：全联零牺牲高命中不颠簸；0.8/0.3 颠簸
vs 0.8/0.9 换血不触发；零插入/零查找哨兵下 thrashing false（首跑红为
哨兵参与比较的实现真缺陷，修正无据不定罪）；负计数/阈值越界与 NaN
fail-fast。

