---
id: T2183
title: Spill 冷热分层访问审计（SpillTieringAudit）的形状裁决
type: task
status: closed
assignee: zcode-l
blocked-by: 
created: 2026-09-14
---

## Question

L 会话第 41 轮：spill 读侧访问频率分层审计面的形状选什么？

## Resolution

**用户常设授权 AFK（可推翻）**

勘察：ReadAuditTrail 有读事件窗（128）无分层聚合；843 引用失效/815 写放大各占一轴——读侧频率分层轴开放。

形状裁决：SpillTieringAudit 纯函数（spill）——analyze(totalHandleUris, readRecords)→TieringReport（never=全集−覆盖 distinct 钳 0/single/multi≥2 热点）+hotRatio/coldRatio 派生（空库 -1 哨兵）；trail 有界窗口径显式；只读不裁决。

Out of scope：自动分层；时间衰减；会话分桶。
