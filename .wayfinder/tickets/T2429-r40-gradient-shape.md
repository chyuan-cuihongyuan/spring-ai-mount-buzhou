---
id: T2429
title: R40 梯度限流器观测接线的形状裁决
type: task
status: closed
assignee: zcode-n
blocked-by: T2428
created: 2026-09-15
---

## Question

N 会话第 40 轮：梯度限流器直接接 tryAcquire 闸还是观测先行？

## Resolution

选 **观测先行**。闸接入改变批并发准入（行为面大——批时延劣化即拒绝工具
执行的语义需要独立验证与配置面）；先把延迟梯度数据显形（View 读数），积累
运行证据后闸接入独立轮裁决。finally 路径保证取消/异常批也入账（数据完整）。
