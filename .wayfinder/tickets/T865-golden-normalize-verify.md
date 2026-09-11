---
id: T865
title: 归一化验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T864
created: 2026-09-12
---

## Question

归一化语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（EventSequenceAssertNormalizationTest 3/3）：

- 五类易变值哨兵化 + 稳定值（字符串/小数字/布尔）原样。
- 嵌套 Map/List 递归 + 键保序。
- 端到端：会话 emitEvent 带易变值 → assertPayloadNormalized 按哨兵结构通过。
