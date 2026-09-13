---
id: T1163
title: 会话准入拒绝分布的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

拒绝原因聚合面怎么做？开集封顶与喂点如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 32 轮 = effort #831 / spec 831 / impl 584）：`SpawnRejectionDistribution`——开集原因键封顶 16+count/lastSeen 降序+dominant；null 忽略；喂点=拒绝事件消费者；SpawnGate 零变更。
