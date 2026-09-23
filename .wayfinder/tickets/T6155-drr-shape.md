---
id: T6155
title: S 会话 S28 Deficit Round Robin 亏空调度的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-24
---

## Question

异构负载排队怎么字节公平且无流饿死？（spec 5027 /
effort #5027 / S28）

## Resolution

**DeficitRoundRobin（core/policy）**：Shreedhar-Varghese DRR
思想——轮转各队 deficit += quantum，队头 size ≤ deficit 发出
扣减，队空亏空清零；亏空跨轮结转；单轮无服务 null 确定性
不阻塞。
