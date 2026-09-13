---
id: T1453
title: 慢调用榜读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 2 轮：工具慢调用榜（Redis SLOWLOG）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 2 轮 = effort #1001 / spec 1001 / impl 754）：缺口成立——spec 108 timer 与 spec 700 ToolTimingAggregator 都是聚合面（P95/分位），「哪几次调用慢、慢在哪个工具、是否伴随失败」的单次现场不可见。落点 core.exec 新公共类 `ToolSlowLog`（Redis SLOWLOG 语义：执行时长**严格大于**阈值 slowlog-log-slower-than 才入榜；有界 FIFO 环 max-len 32，非严格 Top-K 排名）：`record(toolName, elapsedNanos, failed)` 静态接线（HookedToolCallback 与 timer 同点、volatile 阈值比较热路径零担）；`entries()` 新→旧只读快照；`configureThreshold(Duration)` 进程级设置 + `reset()` 测试注入点。默认阈值 1000ms（static final 常量）；失败工具照记（failed 标记，spec 108 outcome 同口径）。
