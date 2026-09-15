# impl 1326 — EpisodeRetentionStats 情节保留普查（R27 = effort #1726 / spec 1726 / T2653-T2654）

**What**：三事件闭集+批量记账+evictRatio 哨兵+reset
**Why**：Kafka retention——TTL vs 容量逐出归因
**Verify**：EpisodeRetentionStatsTest 3 断言 全绿。 **Status**：done（2026-09-15）
