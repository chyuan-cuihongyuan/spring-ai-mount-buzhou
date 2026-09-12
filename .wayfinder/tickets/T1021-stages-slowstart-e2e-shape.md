---
id: T1021
title: 金丝雀过滤×慢启动×热重载联动补验的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

RouteStages/RoutingSlowStart/RoutingWeightsHotReload 三件编排语义未闭环。

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 36 轮 = effort #735 / spec 735 / impl 538，测试域补验轮）：编排用例——filter 保留候选构造路由（archived 即使在 weights 也不入），热重载上调走 ramp，tick 逐步到位。
