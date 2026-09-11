---
id: T904
title: fork 谱系进会话面板的形态裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

spec 602 的 fork 谱系（state 键）如何进运维面板（spec 346 sessions 端点）？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 28 轮 = effort #600 / spec 627 / impl 480）：

1. 端点增 `forkedActive` 段：活跃会话中带 `buzhou.fork.source` 的计数（重试/探索分支流量信号——「一半活跃会话是分支」即重试风暴信号）。
2. 读取口径：按索引 ACTIVE 分页（同计数循环 ≤50k 封顶）逐会话一次 state GET——ops 按需面板可接受；state 读面缺席 = available:false 诚实缺席（五参构造，四参兼容保留）。
3. 谱系键常量端点侧声明 + 测试经真实 fork 双向钉住（写入方 DefaultAgentRuntime 与读取方同键）。
