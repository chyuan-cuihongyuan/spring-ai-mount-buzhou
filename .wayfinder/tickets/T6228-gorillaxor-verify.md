---
id: T6228
title: T 会话 T14 Gorilla XOR 浮点压缩的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6227]
created: 2026-09-26
---

## Question

T14 合同怎么逐一验绿？（spec 6013 / effort #6014 / T14）

## Resolution

**验证通过**：GorillaXorTest 六测全绿——量化步进 500 点
无损+压缩 <1/3；随机 200 点无损；特殊值按位保留；全同
64+99 位恒等钉住；位流确定性；fail-fast。
