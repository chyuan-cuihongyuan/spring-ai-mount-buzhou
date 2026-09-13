---
id: T1530
title: 摘要桥操作与代数回退读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1529
created: 2026-09-14
---

## Question

J 会话第 38 轮：操作与代数回退读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SummaryBridgeStatsTest，复用 ManualCompactorTest 的内存仓+九段式骨架）：save 计 saves；loadLatest 命中计 loads（未命中也计 loads 不计命中——只做操作计数）；代数回退（gen 5 后存 gen 3）计 generationRegressions=1；LRU 1024 上界边界（构造 1025 会话——抽验上界不越）；fresh 零值。定向 `mvn -pl buzhou-memory test -Dtest='SummaryBridgeStatsTest,ManualCompactorTest'` 绿。
