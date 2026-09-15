# impl 1451 — TombstoneRatioReadout 墓碑占比读面（R51 = effort #1850 / spec 1850 / T2901-T2902）

**What**：`TombstoneRatioReadout`（core/cleanup 静态纯函数）——ratioOf →
Ratio（占比 + readAmplification 1/(1−ratio) + shouldCompact 边界含上，
默认阈 0.2）；负计数/阈值越界与 NaN fail-fast。

**Why**：LSM-Tree tombstone/Cassandra compaction 思想——软删除积累的
空间账（死数据背多少）与读放大账（每读跳多少墓碑）双面；过阈即压的
判定面。

**Verify**：`TombstoneRatioReadoutTest` 4 用例全绿（首跑红为测试数据
算术误，修正后绿）。

**Status**：done（2026-09-16）
