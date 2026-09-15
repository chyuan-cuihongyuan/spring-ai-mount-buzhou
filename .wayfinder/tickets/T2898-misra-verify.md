---
id: T2898
title: Misra-Gries 素描的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2897]
created: 2026-09-16
---

## Question]

素描在幸存/下界/确定性/畸形四面下正确吗？（spec 1848 / effort #1848 / R49）

## Resolution

**MisraGriesSketchTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=MisraGriesSketchTest）：多数项（2/3 占比）k=2 幸存且估计 ∈ [2,6]；
下界口径（估计≤真实、误差≤N/k、频项必幸存）；同流同素描+空流/null 空；
k<2 与 null 元素 fail-fast。

