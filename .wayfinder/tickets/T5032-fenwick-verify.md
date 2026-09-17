---
id: T5032
title: Q 会话 R16 Fenwick 树的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5031]
created: 2026-09-18
---

## Question

R16 合同怎么逐一验绿？（spec 3015 / effort #3015 / R16）

## Resolution

**验证通过**：FenwickTreeTest 七测全绿——{3,5,7,−,2} 前缀阶梯
手算、负增量回零、区间和 12/17/0 前缀差三例、1000 随机点更新
逐次对拍朴素数组恒等、空/全前缀与初值零、六路越界 fail-fast、
Long.MAX/2 无溢出。
