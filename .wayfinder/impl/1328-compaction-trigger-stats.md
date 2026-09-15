# impl 1328 — CompactionTriggerStats 压缩触发原因分布（R29 = effort #1728 / spec 1728 / T2657-T2658）

**What**：四因闭集+idleShare 哨兵+reset
**Why**：RocksDB/Cassandra compaction stats——触发阈值调参方向
**Verify**：CompactionTriggerStatsTest 3 断言 全绿。 **Status**：done（2026-09-15）
