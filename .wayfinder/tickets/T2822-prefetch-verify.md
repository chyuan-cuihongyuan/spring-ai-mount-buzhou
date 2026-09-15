---
id: T2822
title: Prefetch 信用窗口的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2821]
created: 2026-09-16
---

## Question]

信用闸在满窗拒/串行/初始/畸形四面下正确吗？（spec 1810 / effort #1810 / R11）

## Resolution

**PrefetchCreditWindowTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=PrefetchCreditWindowTest）：容量 2 满窗双拒+耗拒计数 2+归还续流；
容量 1 串行取还；初始快照零在飞满可用；容量<1 与空窗 release fail-fast。

