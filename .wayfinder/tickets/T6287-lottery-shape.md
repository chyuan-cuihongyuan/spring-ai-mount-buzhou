---
id: T6287
title: T 会话 T44 Lottery Scheduler 彩票调度的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-28
---

## Question

加权调度怎么免 WRR 短窗倾斜与固定轮询无权重？（spec 6044 /
effort #6044 / T44）

## Resolution

**LotteryScheduler（core/concurrent，源码预载）**：票数比例
加权随机（n/总票数获胜概率）；种子化 Random 可回放；
register/remove+ticketsOf 读数；票数≤0/重复/缺席 fail-fast。
