---
id: T6165
title: S 会话 S33 B+ Tree 有序索引的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

有序索引怎么高扇出确定性平衡且叶序可扫？（spec 5032 /
effort #5032 / S33）

## Resolution

**BPlusTree（core/metrics）**：InnoDB B+ 树思想——键值全落
叶层+内节点分隔键（扇出 m），叶 next 链顺序扫；对半分裂
（叶=左半末键复制上提、内=中位键移动上提）；upsert 覆盖；
maxKeys<2 fail-fast。
