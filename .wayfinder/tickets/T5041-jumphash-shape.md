---
id: T5041
title: Q 会话 R21 跳增哈希的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

分桶路由怎么零内存且扩缩容最小迁移？（spec 3020 / effort #3020 / R21）

## Resolution

**JumpConsistentHash（core/policy，纯函数）**：Google jump hash
——线性同余跳增，O(1) 空间零表零虚节点；n→n+1 仅 ≈1/(n+1) 键
迁移且全落新桶。与一致性哈希环成对取舍：环任意增删+权重（虚节点
内存），jump 零内存但只尾端扩缩（契约边界诚实声明）。
