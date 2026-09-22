---
id: T3038
title: 有界旧读的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3037]
created: 2026-09-23
---

## Question)

旧度判定在边界/偏斜/畸形下正确吗？（spec 1918 / effort #1918 / R119）

## Resolution`

**BoundedStalenessTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=BoundedStalenessTest）：达标/超界两例；恰等含上达标；偏斜
钳 0；畸形两型 fail-fast。
