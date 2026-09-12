---
id: T916
title: resilience 观测/装配补验双小件裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

spec 601 的 panic 只有指标计数（fire-and-forget）无编程可读面；spec 620 的 time-window 无 yml 绑定用例。怎么补？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 34 轮 = effort #600 / spec 633 / impl 486）：

1. `ModelOutlierEjection.panicActivations()` getter（AtomicLong 与指标同源计数）——非零持续增长 = 备选池常年低于恐慌线的可编程信号。
2. `circuit.time-window` yml 绑定用例补验（多构造 canonical @ConstructorBinding 已在——纯测试补齐 620 的装配面覆盖）。
