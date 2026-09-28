---
id: U7257
title: U 会话 U29 SortedRunMerge 的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

LSM 合并怎么清墓碑？（spec 7028 / effort #7028 / U29）

## Resolution

**SortedRunMerge（core/metrics）**：k 路归并新覆盖旧+TOMBSTONE 掩埋；游程内后写胜；乱序 fail-fast。
