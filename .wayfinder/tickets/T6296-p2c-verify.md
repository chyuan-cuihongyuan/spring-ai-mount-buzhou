---
id: T6294
title: T 会话 T47 PowerOfTwoChoices 二择一负载均衡的验证裁决
type: task
status: closed
assignee: zcode-t
blocked-by: [T6295]
created: 2026-09-28
---

## Question

T47 合同怎么逐一验绿？（spec 6047 / effort #6047 / T47）

## Resolution

**验证通过**：PowerOfTwoChoicesTest 四测全绿——2 桶
差距 ≤1；1M 球 maxLoad≤4+守恒；同种子序列全等；fail-fast。
（勘误：并列取 min(i,j) 越过严格轻桶+同桶未重抽已修）
