---
id: T6251
title: T 会话 T26 Striped Lock 条带锁的形状裁决
type: task
status: closed
assignee: zcode-t
blocked-by: []
created: 2026-09-26
---

## Question

键级互斥怎么不加每键锁？（spec 6025 /
effort #6025 / T26）

## Resolution

**StripedLock（core/concurrent，源码 T24 预载）**：SplitMix64
混淆+位掩码的固定条带锁池（2 的幂取整）——相近键散开、
热键恒同锁；withLock 重载+stripeCount/indexFor 读数；
stripes≤0 fail-fast。
