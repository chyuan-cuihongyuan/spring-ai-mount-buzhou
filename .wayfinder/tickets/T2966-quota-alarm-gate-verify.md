---
id: T2966
title: 配额空间告警门的落地验证
type: task
status: closed
assignee: zcode-o
blocked-by: [T2965]
created: 2026-09-23
---

## Question)

告警门在写满/恢复/水位/畸形四面下正确吗？（spec 1882 / effort #1882 / R83）

## Resolution`

**QuotaAlarmGateTest 4 用例全绿**（mvn -pl buzhou-core test
-Dtest=QuotaAlarmGateTest）：写满即拒且保持、读恒放行；ack 不足额
保持拒绝/足额复位；usageRatio 精确（0.5/0.97/1.0+）；畸形三型
fail-fast。
