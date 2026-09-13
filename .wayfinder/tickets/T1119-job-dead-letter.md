---
id: T1119
title: 作业死信台账的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

一次性作业失败明细如何停尸？观察者挂接的兼容性语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 10 轮 = effort #809 / spec 809 / impl 562）：`JobDeadLetterLog`+DelayedJobQueue 可选 BiConsumer 观察者（新 2 参构造，null=原行为；观察者异常隔离）——环 64+聚合 64+totalFailed；message 截 200；不重投。换题注记：原不可路由事件题侵入分发主路径——启用 S8。
