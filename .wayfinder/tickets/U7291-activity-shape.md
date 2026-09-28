---
id: U7291
title: U 会话 U46 活动选择贪心的形状裁决
type: task
status: closed
assignee: zcode-u
blocked-by: []
created: 2026-09-29
---

## Question

最大兼容子集怎么贪心最优？（spec 7045 / effort #7045 / U46）

## Resolution

**ActivitySelectionGreedy（core/policy）**：最早结束贪心（交换论证最优）；并列 canonical；[start,end)；倒置 fail-fast。
