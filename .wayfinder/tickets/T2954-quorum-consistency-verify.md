---
id: T2954
title: 法定人数一致性的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2953]
created: 2026-09-22
---

## Question)

法定人数判定在经典配置/边界交集/弱一致/畸形四面下正确吗？（spec 1876 / effort #1876 / R77）

## Resolution`

**QuorumConsistencyTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=QuorumConsistencyTest）：(3,2,2) 强一致交集 1 余量 1；(5,3,3)
交集 1 可挂 2；(3,1,1) 弱一致交集 0；(4,3,2) 边界强一致余量 1；
畸形五型（N=0/R=0/R>N/W=0/W>N）fail-fast。首测交集负值未钳暴露
契约分歧（-1 vs 0），按「0=无保证」口径修正实现。
