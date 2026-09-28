---
id: T6289
title: T 会话 T45 WaitForGraph 等待图死锁检测的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-28
---

## Question

死锁怎么在加边瞬间发现并裁决受难者？（spec 6045 / effort #6045 / T45）

## Resolution

**WaitForGraph（core/concurrent）**：节点=事务、边
waiter→holder，环即死锁——加边即回报规范环（升序 DFS +
最小 id 旋转）与受难者（环内最大 id）；removeNode 打断；
端点未注册/自环/重复 fail-fast。
