---
id: T6234
title: T 会话 T17 Dictionary Encoding 字典编码的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6233]
created: 2026-09-26
---

## Question

T17 合同怎么逐一验绿？（spec 6017 / effort #6017 / T17）

## Resolution

**验证通过**：DictionaryEncodingTest 五测全绿——5 基数
300 行往返+字典序+位宽 3；单值 0 位宽；4 基数 2 位宽；
1000 行双值 16 字对账；fail-fast。
