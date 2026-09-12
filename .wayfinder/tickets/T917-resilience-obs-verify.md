---
id: T917
title: 补验双小件验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T916
created: 2026-09-12
---

## Question

补验语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（PanicTest 8/8 含计数断言 + AutoConfigTest 12/12 含绑定用例；resilience 全模块 241/241 零回归）：

- panic 触发后 panicActivations()=1。
- time-window=90s yml 绑定 = Duration(1m30s)。
