---
id: T2881
title: 向量时钟偏序的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

无中心时钟下「谁先/真冲突」怎么判？（spec 1840 / effort #1840 / R41）

## Resolution

**Dynamo 向量时钟/Lamport happens-before 思想纯判序 `VectorClockOrder`
（core/concurrent）**：compare(a,b) 键域并集逐分量比（缺席按 0 稀疏合法）
→ 三态 BEFORE/AFTER（各分量≤且至少一严格小）/CONCURRENT（互相各有
领先——冲突解判定前提）；相等/双空退化 BEFORE（「不后于」）；负分量
fail-fast。纯判序不合并。

