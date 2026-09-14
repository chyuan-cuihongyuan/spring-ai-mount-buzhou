---
id: T2293
title: MemoryModule yml 解析样板统一（六-5 部分）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 24 轮：六-5（MemoryModule yml 子树解析样板重复 10+ 次）如何去重？

## Resolution

**用户常设授权 AFK（可推翻）**

形状：抽 memoryLeaf(key)（一级叶子，类型不符/缺席 null）与 memorySub(key)（二级子 map，缺席空 map）两 helper——9/13 处样板统一（开关型×3、二级嵌套×2、数值/区间×2、字符串×1、子 map×1）；3 处保留（embedding-provider 反射加载体/windowOverrides 泛型 Map 消费/复杂多级体——非简单提取，强统一降低可读性）。等值重构（行为逐位不变，memory 187 用例零回归）。
