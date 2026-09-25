---
id: T6204
title: T 会话 T2 Splay Tree 伸展树的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6203]
created: 2026-09-25
---

## Question

T2 合同怎么逐一验绿？（spec 6001 / effort #6001 / T2）

## Resolution

**验证通过**：SplayTreeTest 六测全绿——500 键顺序插入中序
全等；扰动混合操作 TreeMap 圣像；访问后 rootKey=被访键；
删除中序不变；双实例同操作同 rootKey 采样；fail-fast。
