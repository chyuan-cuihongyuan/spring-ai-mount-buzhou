---
id: T6232
title: T 会话 T16 Bit Packing 固定位宽打包的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6231]
created: 2026-09-26
---

## Question

T16 合同怎么逐一验绿？（spec 6015 / effort #6016 / T16）

## Resolution

**验证通过**：BitPackingTest 五测全绿——九档位宽往返全等；
宽 7×100 值 11 字跨字逐值钉住；64 位满宽；确定性；
fail-fast（初版 63 位域 limit 溢出勘误）。
