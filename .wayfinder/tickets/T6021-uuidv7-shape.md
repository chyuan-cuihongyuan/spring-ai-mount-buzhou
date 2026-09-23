---
id: T6021
title: R 会话 R11 UUIDv7 的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

字典序即时间序的零协调 ID 怎么生成？（spec 4010 / effort #4010 / R11）

## Resolution

**UuidV7Monotonic（core/concurrent）**：RFC 9562——48 位毫秒时间戳
置最高位（字典序=时间序）+ rand_a 12 位单调计数器（同毫秒不撞序）
+ 溢出借位伪时序推进；时钟/随机源可注入回放；timestampOf/counterOf
回读（version 拒判）。与 Snowflake（协调位）成对：v7 零协调。
