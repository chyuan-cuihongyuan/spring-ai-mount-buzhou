---
id: T6205
title: T 会话 T3 Treap 树堆的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-25
---

## Question

有序映射怎么用单旋转拿到期望平衡？（spec 6002 /
effort #6002 / T3）

## Resolution

**Treap（core/concurrent）**：Seidel-Aragon 思想——BST 键序 +
最小堆优先级双不变量，优先级种子化 SplitMix64（同种子同
结构）；插入单旋上浮、remove 旋降摘叶；size/height 读数 +
keysInOrder；upsert/缺席 null/嫌席 fail-fast。
