---
id: T6055
title: R 会话 R28 稳定匹配的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

双边偏好对接怎么无阻塞对？（spec 4027 / effort #4027 / R28）

## Resolution

**StableMatching（core/policy，纯静态）**：Gale-Shapley 延迟接受
——求婚方按序出价、受婚方持最优拒余者；稳定（无阻塞对）且对
求婚方最优；不齐边/链尽诚实不配。与拓扑排序同族不同问
（顺序 vs 配对）。
