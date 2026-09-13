---
id: T1125
title: 观测管道内存限流器的形态裁决
type: task
status: closed
assignee: zcode-h
blocked-by: []
created: 2026-09-13
---

## Question

内存预算准入放管道内还是独立判定脑？权重单位与拒绝语义如何定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（H 会话第 13 轮 = effort #812 / spec 812 / impl 565）：`PipelineMemoryLimiter` 独立判定脑（CAS 并发安全）——tryAdmit 拒收不记账+release 防负归账；单位无关；拒绝只发信号（策略归调用方）；零负权重恒过不计。
