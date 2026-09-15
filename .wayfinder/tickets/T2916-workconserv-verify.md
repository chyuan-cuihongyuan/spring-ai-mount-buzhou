---
id: T2916
title: 保工作性审计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2915]
created: 2026-09-16
---

## Question]

违例判定在积压闲置/健康/哨兵/畸形四面下正确吗？（spec 1857 / effort #1857 / R58）

## Resolution`

**WorkConservationAuditTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=WorkConservationAuditTest）：2 时隙 1 违例（a 积压 100、b 闲置 5）
覆盖比 0.05+违例率 0.5；全忙/闲无积压零违例；空/null 哨兵；空白名/
负积压/负容量 fail-fast。

