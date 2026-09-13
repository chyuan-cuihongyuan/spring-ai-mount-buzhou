---
id: T1481
title: 超时覆盖命中读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 16 轮：工具超时覆盖命中读面（幽灵覆盖配置检测）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 16 轮 = effort #1015 / spec 1015 / impl 768）：缺口成立——ToolTimeoutOverrides（spec 529）timeoutMillisFor 按 glob 首个命中返回覆盖值，但**零观测**：配置键拼错工具名 = glob 永不命中 = 覆盖静默失效（R3 幽灵禁用同族），且「哪些覆盖在真被命中」不可见。落点 core.exec：新公共 record `ToolTimeoutOverrideStats(lookups, hits, misses, hitsByPattern)`（守恒 hits + misses == lookups；hitsByPattern 按配置模式分桶——永不命中的模式即幽灵覆盖）+ `stats()` 快照；timeoutMillisFor 返回值逐位不变（命中/未命中各计一次，首中即胜口径不变）。
