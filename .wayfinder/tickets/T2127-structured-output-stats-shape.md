---
id: T2127
title: 结构化输出 REASK 读数（StructuredOutputStats）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 14 轮：结构化输出漏斗读面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：REASK 机制已在（spec 19/T87/impl-62，含 runaway 步数闸计入），零读面；grep structuredOutputStats 无撞。

形状裁决：StructuredOutputStats（core/session 公共静态面）——漏斗五计数+双守恒式（attempts=首过+再问解析+失败；reasks=再问解析+失败）+firstPassRate 派生（-1 哨兵）+resetForTest；埋点 DefaultAgentSession.chatForEntity 五点只增记账行为逐位不变；record 方法 public 跨包埋点（类注如实声明内部语义）。

Out of scope：类型分桶；max_retries>1；其他解析点延伸。
