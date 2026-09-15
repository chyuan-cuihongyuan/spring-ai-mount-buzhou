---
id: T2886
title: 隔离区普查的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2885]
created: 2026-09-16
---

## Question]

积压读数在分账/两极/哨兵/畸形四面下正确吗？（spec 1842 / effort #1842 / R43）

## Resolution

**QuarantineCensusTest 4 用例全绿**（mvn -pl buzhou-guard test
-Dtest=QuarantineCensusTest）：2 待审 1 已审+最老旧 900（已审 5000 不计）+
占比 2/3；全已审（最老旧 -1）/全待审（1.0）两极；空表/null 哨兵；空白
id/负龄期 fail-fast。

