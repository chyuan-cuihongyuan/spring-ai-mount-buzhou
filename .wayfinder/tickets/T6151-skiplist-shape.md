---
id: T6151
title: S 会话 S26 Skip List 跳跃表的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

有序动态集合怎么期望对数且实现直白？（spec 5025 /
effort #5025 / S26）

## Resolution

**SkipList（core/metrics）**：Pugh 思想——多层前向指针塔
（层高种子化逐半衰封顶 MAX_LEVEL），put upsert/get/keysInOrder
期望 O(log n)；种子注入确定性。
