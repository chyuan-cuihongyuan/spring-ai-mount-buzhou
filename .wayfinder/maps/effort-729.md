# effort #729 — 内存观测库容量健康面

- 会话：G 会话 700 系第 30 轮 ｜ spec [729](../../../docs/spec/729-observability-capacity-health.md) ｜ 票 [T1058](../tickets/T1058-obs-capacity-health.md)/[T1059](../tickets/T1059-obs-capacity-health-verify.md) ｜ impl629
- 借鉴：—（容量逐出读数接线，548 同型）

## 勘察（排重）

- InMemoryObservabilityStore 容量满逐出最久未活跃会话（volatile-lru 采样）——逐出静默发生，计数与占用无读数；grep -i evicted/容量健康：health 面零命中。

## 决定

store 加 evictedSessions 计数+evictedSessionCount()/sessionCount()（提升 public）/maxSessions() 读数；`ObservabilityCapacityHealth`（core/health）mechanism=memory-observability 恒 UP——details used/max/utilization/evicted。

## 测试

容量 2 写 3 会话→used=2/evicted=1/utilization=1.0；null fail-fast。
