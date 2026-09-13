---
id: T1482
title: 超时覆盖命中读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1481
created: 2026-09-14
---

## Question

J 会话第 16 轮：超时覆盖命中读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（ToolTimeoutOverrideStatsTest，AssertJ 同仓风格）：精确键命中 hits=1 且 hitsByPattern 记到该模式；未命中查计 miss；glob 模式命中记到模式；首中即胜（两模式同配同工具只计首模式）；幽灵模式（拼错名永不命中）hitsByPattern 零/缺席——显形；守恒 hits + misses == lookups；返回值回归（覆盖值与 -1 直通不变）。定向 `mvn -pl buzhou-core test -Dtest='ToolTimeoutOverrideStatsTest,ToolTimeoutOverridesTest'` 绿（后者若存在）。
