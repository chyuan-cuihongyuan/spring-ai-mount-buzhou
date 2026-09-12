---
id: T1011
title: 路由阶段标签 yml 装配的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

RouteStages（spec 723）只有编程面——yml 声明式入口缺失。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 31 轮 = effort #730 / spec 730 / impl 533）：buzhouWeightedChatModel bean 增 Environment 参数——`buzhou.routing.stages.<name>`（map）+ `buzhou.routing.visible-stages`（set）双声明才生效；filter 后 <2 路抛 BuzhouConfigurationException（路由面 ≥2 fail-fast 口径）；缺省零变化。
