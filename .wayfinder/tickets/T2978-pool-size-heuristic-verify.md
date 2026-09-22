---
id: T2978
title: 连接池容量启发的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2977]
created: 2026-09-23
---

## Question)

容量公式在经典/SSD/拆分/饱和/畸形五面下正确吗？（spec 1888 / effort #1888 / R89）

## Resolution`

**PoolSizeHeuristicTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=PoolSizeHeuristicTest）：4 核 2 磁轴→10；SSD 4+0→8；23 预算
拆 4 节点 {6,6,6,5}；饱和度 0.95 与零池哨兵；畸形四型 fail-fast。
