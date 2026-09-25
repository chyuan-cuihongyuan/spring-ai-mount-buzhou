---
id: T6230
title: T 会话 T15 Simple8b 位打包的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6229]
created: 2026-09-26
---

## Question

T15 合同怎么逐一验绿？（spec 6014 / effort #6015 / T15）

## Resolution

**验证通过**：Simple8bTest 五测全绿——混合量级 500 值往返
全等；960 零恰 4 字+尾零截断；60 位值满字选择子 15 钉住；
确定性；fail-fast。
