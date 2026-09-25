---
id: T6218
title: T 会话 T9 BK 树的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6217]
created: 2026-09-26
---

## Question

T9 合同怎么逐一验绿？（spec 6008 / effort #6008 / T9）

## Resolution

**验证通过**：BkTreeTest 四测全绿——book/back 经典例钉住；
150 词库×60 查询×r∈{0,1,2} 暴力圣像全等（集+序）；重复
add 幂等；fail-fast。
