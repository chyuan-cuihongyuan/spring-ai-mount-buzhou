---
id: T6267
title: T 会话 T34 Elevator Scan 电梯扫掠的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

寻道服务怎么免往返空跑？（spec 6033 /
effort #6033 / T34）

## Resolution

**ElevatorScan（core/policy，源码 T30 预载）**：显式磁头+
方向 LOOK 扫掠——同向升序、折返点为该向最远请求；服务后
磁头停末次轨道、方向翻转；TreeSet 重复幂等；越域/null
fail-fast。
