---
id: U7208
title: U 会话 U4 AvlTree AVL 树的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7207]
created: 2026-09-29
---

## Question

U4 合同怎么逐一验绿？（spec 7003 / effort #7003 / U4）

## Resolution

**验证通过**：AvlTreeTest 四测全绿——600 随机操作 vs
TreeMap 圣像；顺序/逆序 200 高度 ≤11；1000 随机 ≤15；
upsert；fail-fast。
