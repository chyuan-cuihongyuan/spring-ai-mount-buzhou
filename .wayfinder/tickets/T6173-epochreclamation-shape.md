---
id: T6173
title: S 会话 S37 Epoch-Based Reclamation 时代回收的形状裁决
type: task
status: closed
assignee: zcode-s
blocked-by: []
created: 2026-09-25
---

## Question

无锁结构节点怎么做到不释放仍被读者引用的内存？（spec 5036 /
effort #5036 / S37）

## Resolution

**EpochReclamation（core/concurrent）**：folly/libcds EBR 思想
——enter 守卫钉住全局时代、retire 记录退休时代、advanceEpoch
显式推进；tryReclaim 只收「时代更早且守卫清零」项（按时代
升序+退休序确定性）；守卫双重关闭/重复退休 fail-fast。
