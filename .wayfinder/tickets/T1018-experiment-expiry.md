---
id: T1018
title: 实验到期自动停的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

实验无生命周期——忘下线=曝光照跑。加到期语义吗？yml 集成做不做？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 10 轮 = effort #709 / spec 709 / impl 609）：构造器扩 expiresAt+Clock（原构造委托零变化）；assign() 惰性判定——过期按未入组返回 null、曝光计独立 `__expired__` 桶（不混 `__unenrolled__`）+每实验一次 WARN+expired 计数；readouts expiredExperiments()/expiresAt(name)。expiresAt 值 null fail-fast。yml 集成留后续（绑定键设计专项）。
