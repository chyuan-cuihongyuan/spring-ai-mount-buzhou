# 729 — 内存观测库容量健康面

> 来源：G 会话第 30 轮 = effort #729（容量逐出的读数接线，548 同型）/ [T1058](../../.wayfinder/tickets/T1058-obs-capacity-health.md) / [T1059](../../.wayfinder/tickets/T1059-obs-capacity-health-verify.md) / impl 629。

## Problem

InMemoryObservabilityStore 容量满时逐出最久未活跃会话——观测数据被覆盖丢弃是静默的：运维不知道逐出在发生、多频繁、容量利用率多少。「为什么这个会话的 trace 没了」的答案在逐出计数里，但计数不存在。

## Solution

- store 加逐出计数（evictedSessions AtomicLong）+三个读数（evictedSessionCount/sessionCount/maxSessions——sessionCount 既有包私有提升 public）。
- `ObservabilityCapacityHealth`（core/health）：mechanism=memory-observability 恒 UP；details used/max/utilization/evicted。
- utilization=1 且 evicted 增长 = 观测数据在被覆盖丢弃——告警归 312 订阅自裁（阈值语义：观测可再生数据，DOWN 不合适）。

## Out of Scope

JDBC/Redis 实现的容量面（前者 DB 管、后者 727/保留策略管）；逐出率时间序列（快照口径）。
