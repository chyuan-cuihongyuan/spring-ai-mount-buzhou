---
id: T1533
title: todo 动作分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 40 轮：todo 动作分布读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 40 轮 = effort #1040 / spec 1040 / impl 792）：缺口成立——TodoTool（spec 06 推演 #5，todo 四动作：list/upsert/remove/clear）call 分发全程零计数：四动作各自使用分布不可见——「clear 被频繁调用」=模型反复清单（任务管理行为异常信号）；与 spec 13 全局调用计数分轴（那轴只有总量，本轴按动作类型）。落点 buzhou-tools todo 包：实例级白名单五桶计数（list/upsert/remove/clear + other——未知动作归 other，基数安全有界）+ 嵌套 record `TodoActionStats(byAction)` + `actionStats()` 快照（Map.copyOf 不可变）。实例级；嵌套类型不动 API 快照；分发行为逐位不变。
