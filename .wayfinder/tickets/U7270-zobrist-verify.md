---
id: U7270
title: U 会话 U35 ZobristHashing 的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7269]
created: 2026-09-29
---

## Question

U35 合同怎么逐一验绿？（spec 7034 / effort #7034 / U35）

## Resolution

**验证通过**：三测全绿——整算 vs 增量全等；xorOut 自逆+空态 0；不同态不同哈希；越域 fail-fast。
