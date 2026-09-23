---
id: T6033
title: R 会话 R17 HNSW 的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

稠密向量 kNN 怎么免暴力全扫？（spec 4016 / effort #4016 / R17）

## Resolution

**HnswBeamSearch（core/memory）**：Malkov-Yashunin 2016——指数
落层（高层稀疏长边）+ 查询顶降贪心 + 第 0 层 beam（ef 宽候选池
不困局部最优）+ 插入逐层 beam 连 M 近邻双向边（超限裁远端）。
与 MinHash 互补（稠密向量 vs 集合相似度）。
