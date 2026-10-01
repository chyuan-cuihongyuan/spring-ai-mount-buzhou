---
id: X10065
title: X 会话 33 PushRelabelMaxFlow 推重标最大流 的形状裁决
type: task
status: closed
assignee: zcode-x
blocked-by: []
created: 2026-10-01
---

## Question

形状怎么定？

## Resolution

PushRelabelMaxFlow（core/concurrent，静态纯函数面）：maxFlow(n,edges,s,t)——preflow 初始化源满推+FIFO 活跃队列 discharge（admissible 推进+重标抬高）——无全局重标/间隙启发的基础面；与 Dinic 同签名同容量域；负容量/源汇同点/越界 fail-fast。
