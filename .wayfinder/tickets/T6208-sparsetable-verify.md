---
id: T6208
title: T 会话 T4 Sparse Table 稀疏表的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6207]
created: 2026-09-25
---

## Question

T4 合同怎么逐一验绿？（spec 6003 / effort #6003 / T4）

## Resolution

**验证通过**：SparseTableTest 四测全绿——17 元素 136 子区间
暴力圣像全等；1000 元素 1000 随机查询线性扫全等；重复/
负值/单元素边界；null/空/倒置/越界 fail-fast。
