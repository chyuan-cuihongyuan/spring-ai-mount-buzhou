---
id: T3153
title: 加权公平调度的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

多流共享出口的权重公平分享怎么原语化？（spec 2026 / effort #2026 / R27）

## Resolution

**DRR 线程安全调度器 `WeightedFairScheduler<T>`（core/exec）**：
registerStream（weight≥1 fail-fast）+enqueue（空流入环 deficit 从零
——空闲不积累特权）+pollNext **粘性轮内消费**（当前流可负担连续出队
不重入账，用尽才让出游标入账下一流 quantum×weight）——长期服务比 ≈
权重比（3:1 长跑收敛），空流退休清账+servedByStream 公平对账面。
