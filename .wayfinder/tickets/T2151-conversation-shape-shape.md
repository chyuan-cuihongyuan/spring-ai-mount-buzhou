---
id: T2151
title: 会话历史形态审计（ConversationShapeAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 26 轮：会话历史结构形态审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：message 域无形态审计面（RepetitionDetector 是内容重复域）；角色分布/结构异常全无读面。

形状裁决：ConversationShapeAudit 纯函数（core/message）——analyze→ShapeReport(roleHistogram 降序典序+consecutiveSameRole 相邻同角色非 TOOL+emptyContent 空 content 且无 toolCalls+maxTurnGap 相邻跳变)+空输入哨兵；TOOL 链连续与带工具调用的空 content 是正常形态口径显式。

Out of scope：内容质量；清洗动作；跨会话聚合。
