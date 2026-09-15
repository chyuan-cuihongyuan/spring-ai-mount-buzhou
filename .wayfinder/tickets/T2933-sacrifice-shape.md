---
id: T2933
title: 缓存牺牲率的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

满缓存换血与真颠簸怎么分诊？（spec 1866 / effort #1866 / R67）

## Resolution`

**体系结构缓存分析惯例（sacrifice ratio/thrashing）纯读面
`CacheSacrificeRatio`（buzhou-resilience/cache）**：CacheAccount 四计数
契约 + sacrificeRatio（插入期驱逐/插入，全联理想 0，零插入 -1 哨兵）+
hitRate（零查找 -1 哨兵）+ thrashing 双条件（牺牲高且命中低=白忙该扩容；
**任一哨兵无判 false——无据不定罪**：初版哨兵 -1 参与比较造成假阳性，
实现期自查修正入档）。

