---
id: T3137
title: 检索强度重排接线的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

MemoryStrengthScore 怎么接进 recall 管线？（spec 2018 / effort #2018 / R19）

## Resolution

**mem0 管线落地 `RecallStrengthReranker`（buzhou-memory recall）**：
finalScore = relevanceWeight×hit.score + (1−relevanceWeight)×强度分
（默认 0.7 相关度为主）——开闭装饰不侵入 RecallSearch 本体，元数据
逐 Hit 供给（null=零强度垫底）；TIME 模式退化纯强度序；排序稳定；
两极退化（1 纯相关度/0 纯强度）。
