---
id: T2173
title: 事件时序单调性审计（EventOrderAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 37 轮：事件时间戳单调性审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：TurnSequenceAudit 管 turn 序号缺号（cleanup 域）——occurredAt 时间单调轴全空；事件溯源不变量（同流单调）无审计面。

形状裁决：EventOrderAudit 纯函数（core/observability）——analyze 单会话事件→Report(inversions 相邻严格倒退对数+maxInversionMillis+firstInversionIndex 定位 -1 哨兵)；等时刻不算逆序；null 时戳跳过不崩；单会话口径显式。

Out of scope：跨会话时序；逆序修复；span 层时序。
