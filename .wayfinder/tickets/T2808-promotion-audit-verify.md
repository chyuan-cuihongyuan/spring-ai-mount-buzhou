---
id: T2808
title: 记忆层代晋升审计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2807]
created: 2026-09-16
---

## Question]

晋升账目在聚合/两极/空表/畸形四类输入下行为正确吗？（spec 1803 / effort #1803 / R4）

## Resolution

**MemoryPromotionAuditTest 4 用例全绿**（mvn -pl buzhou-memory test
-Dtest=MemoryPromotionAuditTest）：聚合+过早晋升只计零保留产出轮（空转轮
不计）；健康零晋升 vs 全过早两极；空表/null 哨兵；负数/去向失恒 fail-fast。

