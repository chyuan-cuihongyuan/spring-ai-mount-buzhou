---
id: T6108
title: S 会话 S4 有界负载一致哈希的验证裁决
type: task
status: closed
assignee: zcode-s
blocked-by: [T6107]
created: 2026-09-24
---

## Question

S4 合同怎么逐一验绿？（spec 5003 / effort #5003 / S4）

## Resolution

**验证通过**：BoundedLoadRingTest 六测全绿——稳定映射；容量
上限封顶溢出探查；总容量耗尽 ISE；removeNode 迁移确定；
未知节点/负容量 fail-fast；确定性回放。
