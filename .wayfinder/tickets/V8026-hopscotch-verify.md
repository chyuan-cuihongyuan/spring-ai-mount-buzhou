---
id: V8026
title: V 会话 V13 HopscotchHashTable 的验证裁决
type: task
status: closed
assignee: zcode-v
blocked-by: [V8025]
created: 2026-09-29
---

## Question

V13 合同怎么逐一验绿？（spec 8012 / effort #8012 / V13）

## Resolution

**验证通过**：三测全绿——put/get/覆值/删除手锚；邻域不变量
全量校验+1000 随机 vs HashMap 圣像+扩容重现（H=4）；fail-fast。
