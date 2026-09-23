# impl 1525 — IdleConnectionReaper 空闲连接收割（R125 = effort #1924 / spec 1924 / T3049-T3050）

**What**：`IdleConnectionReaper`（core/concurrent 静态纯函数）——
reapCandidates（闲置 ≥ maxIdle 入选，闲置最久排前，边界含上）+
idleMillis 闲置时长读数；lastUsed/now 非法/maxIdle < 1 fail-fast。

**Why**：HikariCP idle reaper 语义——长生命周期连接闲置堆积无人
管；统一收割判定面让「哪些该收、闲置多久算闲」有口径，收割执行
归连接池。与 PoolSizeHeuristic（R89 容量公式）互补。

**Verify**：`IdleConnectionReaperTest` 3 用例全绿（混合入选含上/
最旧优先排序/读数与畸形三型 fail-fast）。

**Status**：done（2026-09-23）
