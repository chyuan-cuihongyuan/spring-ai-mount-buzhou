---
id: T2435
title: R43 N 系运维手册段的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2434
created: 2026-09-15
---

## Question

N 会话第 43 轮：运维文档按机制逐节还是按族聚合？

## Resolution

选 **按族聚合（四族一节）**。16xx 机制 30+ 个逐节会使 runbook 膨胀；
按运维动作域聚合（缓存族一起调、护栏族一起审）符合值班查档动线。
每条三要素（配置键/观测读数/失控信号）保持可操作性。
