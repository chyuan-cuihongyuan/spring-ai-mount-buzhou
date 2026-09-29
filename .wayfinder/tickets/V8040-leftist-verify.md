---
id: V8040
title: V 会话 V20 LeftistHeap 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8039]
created: 2026-09-30
---

## Question

V20 合同怎么逐一验绿？（spec 8019 / effort #8019 / V20）

## Resolution

**验证通过**：三测全绿——合并手锚+npl 不变量全量校验；
1000 随机 vs PriorityQueue 圣像；fail-fast。
