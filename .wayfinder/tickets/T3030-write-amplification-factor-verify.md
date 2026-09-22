---
id: T3030
title: LSM 写放大读面的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3029]
created: 2026-09-23
---

## Question)

WAF 在量化/限速/畸形下正确吗？（spec 1914 / effort #1914 / R115）

## Resolution`

**WriteAmplificationFactorTest 4 用例全绿**（mvn -pl buzhou-core
test -Dtest=WriteAmplificationFactorTest）：WAF 3.0 精确/零写入
0.0；压实债 0.1；限速判定两侧；畸形四型 fail-fast。
