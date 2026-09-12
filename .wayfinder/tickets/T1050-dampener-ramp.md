---
id: T1050
title: 健康压权半开中点渐变的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

恢复一步到位易二次跳闸——加半开中点档吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 26 轮 = effort #725 / spec 725 / impl 625）：HALF_OPEN→(floor+declared)/2 向下取整；OPEN floor/CLOSED declared 不变；dampened() 语义=未回满视图。确定性两级阶梯非时间窗（诚实边界）。
