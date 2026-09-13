---
id: T1261
title: 健康加权评分读面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 6 轮：`/actuator/buzhou` 端点逐机制罗列 status——聚合评分读面（K8s probe aggregate 思想：单一分数+分档）是否有缺口？评分口径如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 6 轮 = effort #905 / spec 905 / impl 658）：缺口成立——16+ 机制健康只有逐项罗列，dashboard/告警需要单一可读数字。落点 core.health 新公共纯函数类 `BuzhouHealthScore`：评分口径与既有 `BuzhouHealth` 严格语义对齐——**UP=100、UNKNOWN=50、DOWN=0**（UNKNOWN=未启用非故障，中性 50 不奖不罚；DOWN 严格语义是「无法履行核心职能」），总分=算术平均取整；分档常量 HEALTHY_FLOOR=90 / DEGRADED_FLOOR=70（static final，无魔法数字），返回 record `ScoreReport(score, tier, upCount, downCount, unknownCount, downMechanisms)`（DOWN 清单有界）。纯函数不装配端点（端点接线留装配轮——机制轮/装配轮分离纪律）。
