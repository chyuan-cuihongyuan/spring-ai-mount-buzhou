---
id: T2914
title: 利特尔法则审计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2913]
created: 2026-09-16
---

## Question]

互证在换算/容差/零基线/畸形四面下正确吗？（spec 1856 / effort #1856 / R57）

## Resolution`

**LittlesLawAuditTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=LittlesLawAuditTest）：λ=10/s×W=200ms → L=2；2.05 五 pct 内
CONSISTENT vs 4 DIVERGENT；零基线退化（0.5≤1×1 一致、2.5 分歧）；负值
与 NaN 四型 fail-fast。Javadoc 断行误 `>` 前缀自查修正。

