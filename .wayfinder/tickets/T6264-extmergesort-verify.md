---
id: T6264
title: T 会话 T32 External Merge Sort 外归并排序的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6263]
created: 2026-09-26
---

## Question

T32 合同怎么逐一验绿？（spec 6031 / effort #6031 / T32）

## Resolution

**验证通过**：ExternalMergeSortTest 四测全绿——20 组随机
圣像+游程数；公式边界（4/3/1/0）；单块+重复值；fail-fast。
