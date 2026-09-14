---
id: T2391
title: R21 空闲监控全链接线的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2390
created: 2026-09-15
---

## Question

N 会话第 21 轮：sweep 节拍选哪——定时调度还是轮次节拍？

## Resolution

选 **轮次节拍**（afterTurn 每 32 轮）。零线程零调度器；零会话期不 sweep 语义
正确（无活动无空闲判定需求）；32 轮摊薄 O(会话数) 扫描。发现喂数面
SessionFeaturesHook 本身未装配——三件一次接线（断链补全比单救更深）。
