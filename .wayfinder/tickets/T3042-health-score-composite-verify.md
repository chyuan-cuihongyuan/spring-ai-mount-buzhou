---
id: T3042
title: 复合健康分的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3041]
created: 2026-09-23
---

## Question)

合成与判级在带权/边界/畸形下正确吗？（spec 1920 / effort #1920 / R121）

## Resolution`

**HealthScoreCompositeTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=HealthScoreCompositeTest）：等权 70 DEGRADED；带权倾斜；
三档含下边界（80/50/30）；畸形三型 fail-fast。
