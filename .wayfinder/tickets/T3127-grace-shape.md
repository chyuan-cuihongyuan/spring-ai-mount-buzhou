---
id: T3127
title: 启动豁免窗的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

慢启动期的失败豁免怎么有边界地账户化？（spec 2013 / effort #2013 / R14）

## Resolution

**K8s startup probe 线程安全豁免窗 `StartupGraceTracker`
（core/concurrent）**：begin 锚定（重启重锚窗口重算）+ reportFailure
自动分流（已毕业/窗外/未锚定→计账；未毕业窗内→豁免不计故障账）+
graduate 首次成功毕业幂等（豁免给冷启动不给僵尸）+ activeGraces 受
宽容面 + stats 三计数。
