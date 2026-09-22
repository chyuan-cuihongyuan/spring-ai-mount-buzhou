---
id: T2976
title: 窃取时间读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2975]
created: 2026-09-23
---

## Question)

窃取占比在经典/阈值/哨兵/畸形四面下正确吗？（spec 1887 / effort #1887 / R88）

## Resolution`

**StealTimeReadoutTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=StealTimeReadoutTest）：Δ60/Δ1000=6%；阈值 5% 两侧行为；
Δtotal=0 哨兵 0.0；畸形四型（steal 倒退/total 倒退/负阈值）fail-fast。
