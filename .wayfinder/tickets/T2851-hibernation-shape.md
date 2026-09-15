---
id: T2851
title: 会话休眠分级的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

空闲会话的足迹回收怎么分级可算？（spec 1825 / effort #1825 / R26）

## Resolution

**k8s scale-to-zero/duty-cycling 思想纯判档 `SessionHibernationPolicy`
（core/session）**：Policy(软阈≤硬阈 契约) + band 三档（边界含上）+
profile 画像（唤醒税 0/50ms/2000ms 常量 + 足迹比 1.0/0.5/0.1 常量——
「省多少 vs 醒多慢」可算）+ census 普查（三档计数 + footprintReduction
足迹节省率 -1 哨兵）。纯判档零执行。

