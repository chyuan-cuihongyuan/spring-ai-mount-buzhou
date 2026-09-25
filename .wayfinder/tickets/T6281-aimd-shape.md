---
id: T6281
title: T 会话 T41 AIMD Window 加性增/乘性减窗口的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

自适应限额怎么利用成败信号收敛？（spec 6041 /
effort #6041 / T41）

## Resolution

**AimdWindow（core/ratelimit，源码本轮入档）**：成功 +1 钳
max、失败 ×β 向上取整钳 min——乘性减快速退避、加性增缓慢
探测锯齿收敛；current/min/max/β 读数；参数越域 fail-fast。
