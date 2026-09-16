---
id: T2950
title: 正则风险审计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2949]
created: 2026-09-16
---

## Question]

风险分级在经典形态/叠加/安全不误报/畸形四面下正确吗？（spec 1874 / effort #1874 / R75）

## Resolution`

**RegexRiskAuditTest 4 用例全绿**（mvn -pl buzhou-guard test
-Dtest=RegexRiskAuditTest）：(a+)+ SUSPECT；(a|ab+)+ 三形态 DANGEROUS
（首跑红为期望 2 实 3——心算病理第六次实证）；五个线性/转义模式
SAFE；null fail-fast。

