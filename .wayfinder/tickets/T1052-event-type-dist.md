---
id: T1052
title: 事件类型分布读数的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-13
---

## Question

事件类型刷屏/静默缺失无聚合面——做分布读数吗？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 27 轮 = effort #726 / spec 726 / impl 626）：`EventTypeDistribution.of(List<EventRecord>)` 纯函数——rows 降序+字典序稳定+total/distinctTypes/topType 占比；类型不假设闭集。纯读数。Loki top-k 思想。
