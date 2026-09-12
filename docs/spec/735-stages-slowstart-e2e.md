# 735 — 金丝雀过滤×慢启动×热重载联动补验

> 来源：G 会话第 36 轮 = effort #735（spec 723/702/340 联动补验）/ [T1021](../../.wayfinder/tickets/T1021-stages-slowstart-e2e-shape.md) / [T1022](../../.wayfinder/tickets/T1022-stages-slowstart-e2e-verify.md) / impl 538。

## 背景

RouteStages.filter（spec 723）、RoutingSlowStart（spec 702）、RoutingWeightsHotReload（spec 340）三件分属三轮——「过滤→构造→热调升配爬坡」的编排语义未闭环。

## 目标（测试域补验轮）

- filter 保留的候选构造路由 → 热重载上调走 ramp（floor 起步）→ tick 逐步到位；
- ARCHIVED 候选即使出现在 weights 里也不进路由（filter 前置）。

## 兼容性

纯测试域增量。
