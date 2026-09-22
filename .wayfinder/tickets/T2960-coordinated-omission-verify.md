---
id: T2960
title: 协同遗漏校正的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2959]
created: 2026-09-23
---

## Question)

阶梯补账在快慢响应/盲窗/覆盖率/畸形四面下正确吗？（spec 1879 / effort #1879 / R80）

## Resolution`

**CoordinatedOmissionAuditTest 4 用例全绿**（mvn -pl buzhou-core
test -Dtest=CoordinatedOmissionAuditTest）：阶梯 450→4/100→1/
50→1/0→1；遗漏 3/0/0 盲窗 350/0/0；覆盖率 10/40=0.25 与 1.0；
畸形四型（间隔 0/负时延/校正 0/校正<观测）fail-fast。
