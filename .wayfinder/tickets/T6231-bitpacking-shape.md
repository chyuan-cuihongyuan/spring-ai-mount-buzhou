---
id: T6231
title: T 会话 T16 Bit Packing 固定位宽打包的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

窄值域整数列怎么精确按位压缩？（spec 6015 /
effort #6016 / T16）

## Resolution

**BitPacking（core/message）**：统一位宽 w∈[0,64] 无缝串接，
跨字双字移位合并 O(1) 取值；bitWidth/count/wordCount 读数；
null/w 越域/负值/溢出 fail-fast。
