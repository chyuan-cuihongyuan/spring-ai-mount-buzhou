---
id: T1001
title: 路由慢启动 yml 装配的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

RoutingSlowStart（spec 702）只有编程面——热重载权重上调的爬坡语义对 yml 用户不可用。装配缝怎么落？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 26 轮 = effort #725 / spec 725 / impl 528）：① `BuzhouRoutingProperties` 增 `slowStart` 字段（`buzhou.routing.slow-start`；null=关默认；负值 BuzhouConfigurationException fail-fast 与既有口径一致）；② autoconfig 增 `buzhouRoutingSlowStart` bean（routingConfigured 且 slowStart 非 null 才装配——AutoCloseable，Spring 推断 destroyMethod=close 关停调度器）；③ 热重载 bean 注入 `ObjectProvider<RoutingSlowStart>`（getIfAvailable——null 走既有 2 参路径零变化）。D 会话装配轮模式。
