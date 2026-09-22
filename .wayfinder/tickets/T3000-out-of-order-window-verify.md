---
id: T3000
title: 乱序接纳窗的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2999]
created: 2026-09-23
---

## Question)

三态在窗沿/推进/畸形下正确吗？（spec 1899 / effort #1899 / R100）

## Resolution`

**OutOfOrderWindowTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=OutOfOrderWindowTest）：三态各一例；窗沿边界（恰 FRESH/
恰 LATE 沿）；advance 取大与持平；畸形两型 fail-fast。
