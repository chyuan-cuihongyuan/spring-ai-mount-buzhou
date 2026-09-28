---
id: T6287
title: T 会话 T43 MCS Lock 队列锁的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

高争用互斥怎么本地自旋免缓存行风暴？（spec 6043 /
effort #6043 / T43）

## Resolution

**McsLock（core/concurrent，源码 T42 预载）**：等待者入队后
自旋自己节点 locked 位，前驱解锁仅写后继一次（FIFO 交接）；
tail CAS 入队/摘尾；Node 公开句柄；null fail-fast。
