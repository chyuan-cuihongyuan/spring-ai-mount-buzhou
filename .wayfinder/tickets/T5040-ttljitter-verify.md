---
id: T5040
title: Q 会话 R20 TTL 抖动的验证裁决
type: task
status: closed
assignee: zcode-q
blocked-by: [T5039]
created: 2026-09-18
---

## Question

R20 合同怎么逐一验绿？（spec 3019 / effort #3019 / R20）

## Resolution

**验证通过**：TtlJitterTest 七测全绿——同键跨调用恒等、1000 键
[800,1200] 全夹持、双侧铺开（各 ≥100）、零抖动精确、base=1 兜底
1ms、100 键 ≥10 不同值分散度、四路参数 fail-fast。
