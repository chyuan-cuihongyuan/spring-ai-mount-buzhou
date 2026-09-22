---
id: T2994
title: 事务号余量分级的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2993]
created: 2026-09-23
---

## Question)

四级判定在边界/倒置/畸形下正确吗？（spec 1896 / effort #1896 / R97）

## Resolution`

**XidHeadroomGuardTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=XidHeadroomGuardTest）：四级各一例；边界含上（恰 warnAt
即 WARN/恰 limit 即 EXHAUSTED）；余量读数与超发负值；畸形三型
fail-fast。
