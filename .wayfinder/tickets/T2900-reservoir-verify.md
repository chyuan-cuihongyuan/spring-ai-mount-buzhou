---
id: T2900
title: 水库采样的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2899]
created: 2026-09-16
---

## Question]

采样在全量/定容成员/确定性/畸形四面下正确吗？（spec 1849 / effort #1849 / R50）

## Resolution

**ReservoirSampleTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ReservoirSampleTest）：短流保序+k=0/null 空；1000 流采 10 恰 10 且
成员性；同种子同样本+异种子同样合法；负 k/null 元素 fail-fast。

