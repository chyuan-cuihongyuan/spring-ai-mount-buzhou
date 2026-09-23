---
id: T6032
title: R 会话 R16 Slab 装箱的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6031]
created: 2026-09-23
---

## Question

R16 合同怎么逐一验绿？（spec 4015 / effort #4015 / R16）

## Resolution

**验证通过**：SlabClassPackerTest 五测全绿——96/1.25/1024 十二档
几何表（封顶 1024）；恰界归本档三例 + 超块 −1 拒收；浪费比
0/23÷120/NaN 三面；per-class 记账 + 拒收不记账；畸形七型
fail-fast。首版 allocationsIn 笔误已修（无意义三元式改界检直读）。
