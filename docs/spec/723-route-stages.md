# 723 — 路由金丝雀阶段标签

> 来源：G 会话第 24 轮 = effort #723（借鉴 MLflow model stages / argo-rollouts canary）/ [T997](../../.wayfinder/tickets/T997-route-stages-shape.md) / [T998](../../.wayfinder/tickets/T998-route-stages-verify.md) / impl 526。

## 背景

加权路由（spec 339/340）+ 慢启动（spec 702）覆盖了配比与爬坡——但端点的**生命周期阶段**（灰度中/已退役）无表达：退役端点残留在候选面、灰度端点对非实验流量可见。MLflow 的模型阶段（staging/production/archived）是生命周期的显式声明。

## 目标

- `RouteStages`（routing 包）：route → Stage{STABLE, CANARY, ARCHIVED} 有界注册表（cap 64——基数有界纪律；`tag`/`stageOf`（null=未标注）/`view` 不可变快照）。
- 纯函数 `RouteStages.filter(weights, stages, visibleStages)`：**构造期过滤**——stage 不在 visible 集的路由剔除出候选权重表；未标注路由恒保留（默认零变化）；剔除发 `buzhou.routing.stage-filtered` 计数（tag stage）。
- 与既有裁决一致：WeightedChatModel 候选面构造期定死不变——filter 是构造前的选型步骤；灰度放量由权重热调 + 慢启动承接。

## 非目标

不做运行时 stage 热切换（阶段变更 = 重新构造路由——与候选面定死裁决一致）；不做百分比流量分割（权重语义已覆盖）。

## 测试

三态过滤、visible 集条件可见、未标注保留、空结果不抛、cap 拒绝、view 不可变。

## 兼容性

纯增量；不标注 = 零变化。
