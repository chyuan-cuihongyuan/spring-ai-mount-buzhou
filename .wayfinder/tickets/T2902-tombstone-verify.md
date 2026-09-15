---
id: T2902
title: 墓碑占比的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2901]
created: 2026-09-16
---

## Question]

占比面在双账/边界/阈含上/畸形四面下正确吗？（spec 1850 / effort #1850 / R51）

## Resolution

**TombstoneRatioReadoutTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=TombstoneRatioReadoutTest）：0.5 占比 2 倍放大+0.01 健康 1.01×；
全空 1×/全墓碑无穷；阈边界含上（0.2/0.2 true、0.21 false）；负计数/
阈值越界与 NaN fail-fast。首跑红为测试数据算术误（0.2 写 0.25）修正。

