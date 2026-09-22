---
id: T3014
title: 分片偏斜审计的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T3013]
created: 2026-09-23
---

## Question)

偏斜比在量化/触发/畸形下正确吗？（spec 1906 / effort #1906 / R107）

## Resolution`

**ShardSkewAuditTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=ShardSkewAuditTest）：偏斜比 1.5 与 2.45 精确；阈值两侧行
为；绝对均衡 1.0；畸形三型（空表/全零/负负载）fail-fast。
