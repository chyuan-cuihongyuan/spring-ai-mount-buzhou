---
id: T1129
title: MCP 断路器变迁台账的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

server 级变迁史怎么记？检测点与瞬时态口径如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 15 轮 = effort #814 / spec 814 / impl 567）：`McpBreakerTransitionJournal`+可选 2 参构造挂接——三路径后 stateOf 差分入账（同态忽略）；环 64+聚合 32；采样型差分（HALF_OPEN 瞬时不可见——口径显式）；null=原行为。
