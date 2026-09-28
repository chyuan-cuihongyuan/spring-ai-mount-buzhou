---
id: U7244
title: U 会话 U22 WriteAheadLog 的验证裁决
type: task
status: closed
assignee: zcode-u
blocked-by: [U7243]
created: 2026-09-29
---

## Question

U22 合同怎么逐一验绿？（spec 7021 / effort #7021 / U22）

## Resolution

**验证通过**：四测全绿——roundtrip；滚动 2/2/1；损坏 fail-fast 携 LSN；fail-fast。
