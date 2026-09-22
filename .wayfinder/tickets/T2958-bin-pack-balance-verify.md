---
id: T2958
title: 装箱平衡的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2957]
created: 2026-09-23
---

## Question)

FFD 装箱在经典/完美/空表/浪费率/畸形五面下正确吗？（spec 1878 / effort #1878 / R79）

## Resolution`

**BinPackBalanceTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=BinPackBalanceTest）：[4,3,3,2,2]/6 → 3 箱载荷 {6,6,2}；
[3,3,3]/9 → 1 箱 0 浪费；空表 0 箱哨兵；浪费率 2/9 精确；畸形
三型 fail-fast。
