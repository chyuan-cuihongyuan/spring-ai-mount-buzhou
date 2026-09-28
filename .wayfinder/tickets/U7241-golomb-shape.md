---
id: U7241
title: U 会话 U21 GolombRiceCodec 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

偏斜值域怎么变长？（spec 7020 / effort #7020 / U21）

## Resolution

**GolombRiceCodec（core/message）**：商 unary+余数 k 位；BitStream；k∈[0,30]；负值/越流 fail-fast。
