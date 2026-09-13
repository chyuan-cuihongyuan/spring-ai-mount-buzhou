---
id: T1479
title: Spotlighting 应用与损坏计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 15 轮：Spotlighting 应用与损坏计数读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 15 轮 = effort #1014 / spec 1014 / impl 767）：缺口成立——Spotlighting（spec 11，MSRC 提示防御）是纯静态工具类零计数：防御**是否真的在生效**（wrap 被调用多少次）与**解包损坏**（含标记头但结构不完整被原样放行——篡改/截断信号）均不可见。落点 core.hook：进程级静态三计数 `wrapped`（入口含 BEGIN_HEAD 即计尝试）/`unwrapped`（成功还原）/`malformed`（含头但结构不完整原样放行）——守恒 wrapped == unwrapped + malformed；新公共 record `SpotlightingStats` + `Spotlighting.stats()` + `resetForTest()`（静态进程态，BuzhouMetricsHolder 先例）。行为逐位不变（计数不改变任何返回值）。AtomicLong 热路径可承受（wrap 每工具结果一次）。
