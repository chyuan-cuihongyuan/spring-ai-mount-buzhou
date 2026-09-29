---
id: V8013
title: V 会话 V7 DinicMaxFlow 的形状裁决
type: task
status: closed
assignee: zcode-v
blocked-by: []
created: 2026-09-29
---

## Question

容量网络的承载上限怎么一次成型？（spec 8006 / effort #8006 / V7）

## Resolution

**DinicMaxFlow（core/concurrent）**：BFS 分层+当前弧阻塞
流阶段循环；long 容量域；流值唯一承诺（分布不唯一明示）；
负容量/源汇同点 fail-fast。
