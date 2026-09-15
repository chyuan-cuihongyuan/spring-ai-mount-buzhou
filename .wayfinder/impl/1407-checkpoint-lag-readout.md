# impl 1407 — CheckpointLagReadout 检查点滞后读面（R7 = effort #1806 / spec 1806 / T2813-T2814）

**What**：`CheckpointLagReadout`（core/recovery 静态纯函数）——SessionLag 单
会话事实（构造器核契约）+ analyze → LagReport（totalLag/maxLag(-1 哨兵)/
laggiestUser 并列取首 + sessionsBeyond(threshold)/caughtUpRatio(-1 哨兵)）。

**Why**：Kafka consumer-group lag / SQLite WAL checkpoint 思想——产出与检查点
两套水位的差 = 崩溃重放成本；maxLag 是恢复 SLA 上界、越限会话数是风险面、
追平率是检查点节奏健康度，三个读数让检查点频率不用拍脑袋。

**Verify**：`CheckpointLagReadoutTest` 5 用例全绿。

**Status**：done（2026-09-16）
