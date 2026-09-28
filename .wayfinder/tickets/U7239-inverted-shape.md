---
id: U7239
title: U 会话 U20 InvertedIndex 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

布尔检索怎么免全扫？（spec 7019 / effort #7019 / U20）

## Resolution

**InvertedIndex（core/metrics）**：词项→有序 posting，AND 交/OR 并；幂等替换（静默卸载核）；缺席删 fail-fast。
