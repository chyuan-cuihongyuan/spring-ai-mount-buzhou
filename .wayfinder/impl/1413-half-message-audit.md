# impl 1413 — HalfMessageAudit 半消息审计（R13 = effort #1812 / spec 1812 / T2825-T2826）

**What**：`HalfMessageAudit`（core/transaction 静态纯函数）——Intent 三态事实
（HALF/COMMITTED/ROLLED_BACK 契约构造）+ audit(staleThreshold) → Census
（halves/committed/rolledBack/staleHalves 超阈回查候选，阈含边界）+
resolutionRatio/pendingRatio（-1 哨兵）。

**Why**：RocketMQ 事务消息思想——半消息两阶段让「发消息」与「做事务」
原子（本地事务成功才放行），回查兜底滞留；审计读面给未裁决面/回查候选/
裁决健康度三个读数，与 CompensatingBatch（事后补偿）互补成事前事后闭环。

**Verify**：`HalfMessageAuditTest` 4 用例全绿。

**Status**：done（2026-09-16）
