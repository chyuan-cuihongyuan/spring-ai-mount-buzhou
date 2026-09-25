---
id: T6261
title: T 会话 T31 Buddy Allocator 伙伴分配器的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

变长块分配怎么收敛外部碎片？（spec 6030 /
effort #6030 / T31）

## Resolution

**BuddyAllocator（core/memory，单位模型）**：order 无块时
更大块对半分裂（低半自用高半入闲链），释放与伙伴 XOR 2^k
逐级合并；TreeSet 闲链同阶取最小偏移；双重释放/池满
fail-fast。
