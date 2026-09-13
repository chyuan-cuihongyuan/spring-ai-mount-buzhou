---
id: T1466
title: 凭证租约生命周期计数读面的验证裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1465
created: 2026-09-14
---

## Question

J 会话第 8 轮：生命周期计数读面如何验证？

## Resolution

**用户常设授权 AFK（可推翻）**

验证裁决（SecretLeaseStatsTest，Clock 注入固定步进钟全确定性）：issue/renew 成功 → renewed=1；过期后续租抛 IllegalStateException 且 renewRejected=1；不存在租约续租同样计 renewRejected；resolve 过期惰性剔除 → expired=1；revoke 幂等只计一次；stats() 五字段与既有三 getter 对齐恒等。定向 `mvn -pl buzhou-core test -Dtest='SecretLeaseStatsTest,SecretLeasesTest'` 绿。
