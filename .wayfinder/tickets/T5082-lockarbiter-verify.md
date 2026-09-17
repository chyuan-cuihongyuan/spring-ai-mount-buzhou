---
id: T5082
title: Q 会话 R41 时间戳锁的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5081]
created: 2026-09-18
---

## Question

R41 合同怎么逐一验绿？（spec 3040 / effort #3040 / R41）

## Resolution

**验证通过**：TimestampLockArbiterTest 九测全绿——空闲即授、
wait-die 老等少死双向（持有者不动/伤自身）、wound-wait 老伤少等
双向（伤持有者+被伤者重启后再请求只能等）、释放非持有者无副作用
+再授、同 txn 幂等、同钟 txnId 决、null fail-fast。
