---
id: T2827
title: TTL 探针状态机的形状裁决
type: task
status: closed
assignee: zcode-o
blocked-by: []
created: 2026-09-16
---

## Question]

被动式健康判态（到期不靠巡检）+ 可运营梯度怎么安放？（spec 1813 / effort #1813 / R14）

## Resolution

**Consul health check TTL 思想纯判态 `TtlProbeStateMachine`（core/health）**：
evaluate(age, ttl, warnFraction) 三态 PASSING/STALE/CRITICAL（到期线与预警线
均含上）；freshness 剩余新鲜度（1−age/ttl 到期钳 0）；census 三态普查
（criticalRatio -1 哨兵）。纯判态零轮询——过期靠时间自然到期判定。

