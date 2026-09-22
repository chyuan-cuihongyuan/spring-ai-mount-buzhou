---
id: T2988
title: 幂等键判定面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2987]
created: 2026-09-23
---

## Question)

三态判定与 TTL 在边界/畸形下正确吗？（spec 1893 / effort #1893 / R94）

## Resolution`

**IdempotencyKeyGuardTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=IdempotencyKeyGuardTest）：三态各一例；TTL 边界恰到期失效/
差一毫秒存活；null 存档与 null 指纹口径；畸形两型 fail-fast。
