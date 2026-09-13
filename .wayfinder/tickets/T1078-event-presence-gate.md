---
id: T1078
title: 事件静默缺失门的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question
「该发生而没发生」无检测面——加静默缺失门吗？

## Resolution
**用户常设授权 AFK（可推翻）**

决策（G 会话第 40 轮 = effort #739 / spec 739 / impl 639）：`EventTypePresenceGate.gate(events, expectedTypes)` 纯函数——missing 字典序+expectedCount/observedTypes；空契约不误报。纯读数（726 的对偶面：什么没发生）。
