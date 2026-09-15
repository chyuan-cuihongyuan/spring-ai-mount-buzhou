---
id: T2844
title: 优先级反转暴露的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2843]
created: 2026-09-16
---

## Question]

反转账在计数/同级/未知/哨兵/畸形五面下正确吗？（spec 1821 / effort #1821 / R22）

## Resolution

**PriorityInversionExposureTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=PriorityInversionExposureTest）：2 反转（gap 9/2）+同级不反转+比率；
持有者更关键零反转+未知资源诚实账；空表/null 哨兵；空白 id fail-fast。

