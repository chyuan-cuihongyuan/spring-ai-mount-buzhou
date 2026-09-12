---
id: T997
title: 路由金丝雀阶段标签的形态裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

加权路由（spec 339/340）+ 慢启动（spec 702）有了配比与爬坡——但「哪些端点处于灰度/退役阶段、哪些对当前环境可见」无选型面：金丝雀端点混在候选里平权参与路由。MLflow model stages / Argo Rollouts 的 stage 语义怎么映射（与「候选面构造期定死」的既有裁决如何兼容）？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 24 轮 = effort #723 / spec 723 / impl 526）：`RouteStages`（routing 包）——route → Stage{STABLE, CANARY, ARCHIVED} 有界注册表（cap 64；tag/view/stageOf，null=未标注）。纯函数 `RouteStages.filter(weights, stages, visibleStages)`：**构造期过滤**——不在 visible 集的路由剔除出候选权重表（默认未标注 = 可见，零变化）；剔除发 `buzhou.routing.stage-filtered` 计数（tag stage）。与既有裁决一致：WeightedChatModel 不动（候选面仍构造期定死——filter 是构造前的选型步骤）；灰度放量仍由权重热调 + 慢启动承接。借鉴 MLflow model stages（None/staging/production/archived）+ Argo Rollouts canary。
