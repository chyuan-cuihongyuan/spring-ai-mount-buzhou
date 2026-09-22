# impl 1506 — ChunkCompressionPolicy 分块压缩策略（R106 = effort #1905 / spec 1905 / T3011-T3012）

**What**：`ChunkCompressionPolicy`（core/cleanup 静态纯函数）——
shouldCompress（块龄 ≥ 阈值边界含上）+ savingsEstimate
（original×(1−1/ratio)）+ readPenaltyFactor（读放大 = ratio）；
负值/ratio≤1 fail-fast。

**Why**：TimescaleDB chunk 压缩语义——历史治理只有删除一条路太
粗；不可变冷块压缩换空间、近期块保原样不伤写、读代价 = 压缩比
的诚实预期。与保留策略（删除）互补。落轮 grep 复核快速重传族
（FastRetransmitTrigger）占坑换静脉。

**Verify**：`ChunkCompressionPolicyTest` 4 用例全绿（阈值含上/节省
2/3 精确/读代价直读/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
