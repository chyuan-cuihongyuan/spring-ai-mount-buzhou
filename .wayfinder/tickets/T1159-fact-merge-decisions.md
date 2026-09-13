---
id: T1159
title: 事实合并决策分布的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

对账三分决策的统计面怎么做？喂点与有界语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 30 轮 = effort #829 / spec 829 / impl 582）：`FactMergeDecisionDistribution`——9 段×3 决策闭集记账（27 格天然有界）；supersededRatio+段行声明序只含触碰段；喂点=EVENT_RECONCILED 消费者；reconcile 零变更。
