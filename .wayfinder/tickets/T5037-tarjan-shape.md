---
id: T5037
title: Q 会话 R19 Tarjan SCC 的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

循环依赖怎么指认环成员？（spec 3018 / effort #3018 / R19）

## Resolution

**TarjanSccFinder（core/concurrent）**：Tarjan 单遍 SCC（index/
lowlink+显式栈），组件凝聚图反拓扑序输出+cyclicVertices 升序环
成员指认（size>1 或自环单点）——TopologicalSorter 只报有环不指
认谁的留白补位；迭代帧栈实现（2 万深链不爆调用栈）。
