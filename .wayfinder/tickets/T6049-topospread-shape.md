---
id: T6049
title: R 会话 R25 拓扑散布的形状裁决
type: task
status: closed
assignee: zcode-r
blocked-by: []
created: 2026-09-23
---

## Question

跨域容量「都堆热域」怎么静态硬约束？（spec 4024 / effort #4024 / R25）

## Resolution

**TopologySpreadPlacer（core/policy）**：K8s maxSkew——放置后全域
斜度（max−min）≤ maxSkew 才可放（拥挤域截止）；候选序数升序+
域名序（先填最空确定性）；skewOf 读数。与一致性哈希环正交
（容量面 vs 键空间面）。
