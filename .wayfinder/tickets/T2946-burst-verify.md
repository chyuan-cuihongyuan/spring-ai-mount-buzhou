---
id: T2946
title: 突发信用账户的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2945]
created: 2026-09-16
---

## Question]

信用账户在蓄水/透支/余量/畸形四面下正确吗？（spec 1872 / effort #1872 / R73）

## Resolution`

**BurstCreditAccountTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=BurstCreditAccountTest）：50+200 钳 300 满水；透支见底枯竭计数
1+拒后回血再花；250/2=125ms 余量；容量/速率<1、负 cost、时钟回拨
fail-fast。

