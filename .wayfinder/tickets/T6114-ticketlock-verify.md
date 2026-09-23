---
id: T6114
title: S 会话 S7 Ticket Lock 票据锁的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6113]
created: 2026-09-24
---

## Question

S7 合同怎么逐一验绿？（spec 5006 / effort #5006 / S7）

## Resolution

**验证通过**：TicketLockTest 四测全绿——票据算术与乱序
unlock IAE；并发互斥（N 线程计数器最终精确）+ 全员获释 +
queueLength 归零；确定性算术回放；临界区可重入计数外置口径
（不重入——自旋锁语义诚实入档）。
