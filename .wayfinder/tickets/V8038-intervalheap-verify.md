---
id: V8038
title: V 会话 V19 IntervalHeap 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8037]
created: 2026-09-30
---

## Question

V19 合同怎么逐一验绿？（spec 8018 / effort #8018 / V19）

## Resolution

**验证通过**：三测全绿——乱序全序一致性手锚；1000 随机 vs
TreeMap 圣像；双端交替；fail-fast。
