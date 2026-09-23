---
id: T6038
title: R 会话 R19 CoDel 的验证裁决
type: task
status: closed
assignee: zcode-r
blocked-by: [T6037]
created: 2026-09-23
---

## Question

R19 合同怎么逐一验绿？（spec 4018 / effort #4018 / R19）

## Resolution

**验证通过**：CoDelControllerTest 五测全绿——目标下直过 + 回标
复位（dropCount 归零）；持续超载观察窗语义（首超起算/窗内不丢/
超窗首丢 dropCount=1/递缩间隔内不丢/第二丢 dropCount=2）；队列
门面陈头丢新头过 + dropped/passed/depth 账面；百条新鲜流零丢
全过；畸形五型 fail-fast。嵌套 Queue 与 java.util.Queue 类型
遮蔽冲突以 ArrayDeque 字段直用化解。
