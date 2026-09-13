---
id: T1179
title: 泄漏疑似对象聚合器的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

泄漏报告聚合面怎么做？稳键与挂接语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 40 轮 = effort #839 / spec 839 / impl 592）：`LeakSuspectAggregator` 实现 LeakListener——描述截 64 稳键聚合 count/maxAge/lastSeen；键封顶 32+溢出桶；快照降序典序破平；检测器零变更。换题注记：原滚动导出统计半撞启用 S9。
