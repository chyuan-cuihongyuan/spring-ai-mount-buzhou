---
id: T2870
title: ETA 投影的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2869]
created: 2026-09-16
---

## Question]

投影在外推/哨兵/完成/取整/畸形五面下正确吗？（spec 1834 / effort #1834 / R35）

## Resolution

**EtaProjectionTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=EtaProjectionTest）：10/40×100ms → ETA 300/总 400；done=0 或
elapsed=0 哨兵；已完成 ETA 0；3/10×100ms 取整 234；total<1/done 越界/
负 elapsed fail-fast。

