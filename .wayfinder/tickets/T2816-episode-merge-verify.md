---
id: T2816
title: 追限事件会话化的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2815]
created: 2026-09-16
---

## Question

会话化在并段/乱序/单点/哨兵/畸形五面下正确吗？（spec 1807 / effort #1807 / R8）

## Resolution

**ViolationEpisodeMergerTest 5 用例全绿**（mvn -pl buzhou-core test
-Dtest=ViolationEpisodeMergerTest）：并段切段+最长段账目；乱序同语义；单点
零 span 段；空表/null 哨兵；负 gap/null 时点 fail-fast。

