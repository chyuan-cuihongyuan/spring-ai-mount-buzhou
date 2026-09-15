---
id: T2848
title: O 系 R24 对账轮的验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2847]
created: 2026-09-16
---

## Question]

全仓 clean verify BUILD SUCCESS 且三门全绿吗？（spec 1823 / effort #1823 / R24）

## Resolution]

**mvn clean verify BUILD SUCCESS 一次过绿**（README 行先落策略生效
——对比 R18 漏行双红）+ 快照门绿（五新类型一致）+ 对账门四断言绿。
