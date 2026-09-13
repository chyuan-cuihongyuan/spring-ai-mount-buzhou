---
id: T1507
title: 模型窗口解析分布读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 29 轮：模型窗口解析分布读面在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 29 轮 = effort #1028 / spec 1028 / impl 781）：缺口成立——TableContextWindowResolver（spec 707：内置模型窗表 + yml 覆盖 + 未知模型回退 32K 每模型告警一次）解析路径零计数：**override/内置表/回退三路各命中多少**、实际解析过的模型窗分布不可见——「配置的 yml 覆盖是否真被命中（模型名拼错=幽灵覆盖）」「未知模型回退 32K 的面有多大」无读数（告警每模型只一条，量级不可见）。落点 core/token：实例级 overrideHits/builtInHits/fallbackHits 三 AtomicLong + resolvedWindows 有界快照（模型名源自应用配置，基数有界）+ 嵌套 record `WindowResolutionStats` + `stats()`。解析返回值逐位不变。
