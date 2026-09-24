---
id: T6188
title: S 会话 S44 CRDT PN-Counter 正负计数器的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6187]
created: 2026-09-25
---

## Question

S44 合同怎么逐一验绿？（spec 5043 / effort #5043 / S44）

## Resolution

**验证通过**：CrdtPnCounterTest 六测全绿——增减取值+单调表；
纯减可负；三律（交换/幂等/结合值+表全等）；两副本乱序同步
收敛；null/空节点/负量/null other fail-fast。
