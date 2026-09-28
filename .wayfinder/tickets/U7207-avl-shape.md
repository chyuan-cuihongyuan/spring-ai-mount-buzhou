---
id: U7207
title: U 会话 U4 AvlTree AVL 树的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

BST 怎么严格平衡不退化？（spec 7003 / effort #7003 / U4）

## Resolution

**AvlTree（core/concurrent）**：节点缓存高，回溯重平衡
LL/RR/LR/RL，平衡因子 ∈{−1,0,1}——高度界 1.44·log₂(n)；
upsert/缺席删 fail-fast；旋转确定性。
