---
id: T3152
title: 一致性哈希环的验证裁决
type: task
status: closed
assignee: zcode-p
blocked-by: [T3151]
created: 2026-09-17
---

## Question

ConsistentHashRing 合同（确定性/均衡/最小迁移/回绕/畸形）怎么钉住？（spec 2025 / effort #2025 / R26）

## Resolution

**七用例一次全绿**（buzhou-core）：同键确定性 / 三节点 3 万键份额均
∈[20%,47%] / 删 n2 迁移量恰=其原份额且变键原归属必 n2、目标只在幸存
者 / 加 n3 吸收 ∈(0,5000) 且新键必归 n3 / 空环 null / 单虚节点回绕
100 键全归 / 畸形六型（0 虚节点、null/空白节点、重复、删幽灵、
null key）fail-fast。
