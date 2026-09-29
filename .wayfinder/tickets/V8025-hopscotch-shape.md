---
id: V8025
title: V 会话 V13 HopscotchHashTable 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

开放定址探测链怎么钉死在常数内？（spec 8012 / effort #8012 / V13）

## Resolution

**HopscotchHashTable（core/cache）**：H 位邻域位图+swap 回跳
搬元素进邻域+超载扩容 ×2；put upsert/get 诚实/remove fail-fast
三面；邻域不变量可全量校验。
