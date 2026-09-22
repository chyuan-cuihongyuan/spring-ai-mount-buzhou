---
id: T2996
title: 多类型载荷预算分账的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2995]
created: 2026-09-23
---

## Question)

水填分配在经典/宽松/权重/畸形下正确吗？（spec 1897 / effort #1897 / R98）

## Resolution`

**PayloadBudgetSplitTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=PayloadBudgetSplitTest）：经典 {30,50,70}/100 等权 {30,35,35}
零头 0；宽松全额授予；权重 3:1 倾斜；畸形四型 fail-fast。
