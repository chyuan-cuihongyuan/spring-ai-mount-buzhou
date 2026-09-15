---
id: T2930
title: 可见性超时账的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2929]
created: 2026-09-16
---

## Question]

时限账在重投边界/死信边界/三段普查/畸形四面下正确吗？（spec 1864 / effort #1864 / R65）

## Resolution`

**VisibilityTimeoutAccountingTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=VisibilityTimeoutAccountingTest）：1299 false/1300 true/已确认
false；2/3 false、3/3 true、0/0 true；4 消息三段 1/1/1+最老 100+占比
0.25；空/null 哨兵+负时间戳/负计数 fail-fast。

