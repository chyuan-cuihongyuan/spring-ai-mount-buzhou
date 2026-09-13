---
id: T1461
title: 工具在飞并发水位读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 6 轮：工具在飞并发水位读面（Go runtime NumGoroutine / Hystrix）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 6 轮 = effort #1005 / spec 1005 / impl 758）：缺口成立——并发**限流**面已三处（spec 05 信号量、LaneLimitingToolCallback、spec 84 舱），但并发**测量**面为零：spec 127 是 Turn 级跨实例聚合、无单工具进程内在飞水位；「哪个工具卡住了 / 峰值并发到过多少」不可见。落点 core.exec 新公共类 `ToolInFlight`：静态 `enter(toolName)` 发 AutoCloseable 租约（close 恰一次，AtomicBoolean 守卫）→ 每工具 current/peak/total + 全局 currentTotal/peakTotal（CAS max）；`snapshot()` 不可变快照（per-tool map 以工具目录为界——ToolTimingAggregator 同先例）；`reset()` 测试注入点。HookedToolCallback try/finally 同点接线。
