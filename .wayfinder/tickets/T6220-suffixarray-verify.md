---
id: T6220
title: T 会话 T10 Suffix Array 后缀数组的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6219]
created: 2026-09-26
---

## Question

T10 合同怎么逐一验绿？（spec 6009 / effort #6009 / T10）

## Resolution

**验证通过**：SuffixArrayTest 四测全绿——banana 经典 sa/lcp
全序钉住；300 随机文本 100 查询暴力全等；后缀序两两有序
+ LCP 直算全等；fail-fast。
