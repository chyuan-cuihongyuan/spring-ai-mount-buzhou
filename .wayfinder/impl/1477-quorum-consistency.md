# impl 1477 — QuorumConsistency 法定人数一致性（R77 = effort #1876 / spec 1876 / T2953-T2954）

**What**：`QuorumConsistency`（core/transaction 静态纯函数）——
strongConsistency（R+W>N）+ overlapCount（R+W−N 负值钳 0）+
tolerableWrite/ReadFailures（N−W / N−R）+ consistentAvailability
（min 双余量）；N≥1、法定人数越界 fail-fast。

**Why**：Dynamo/Cassandra quorum 交集语义——R/W 配置随手拍弱了读到
旧值、拍强了写一个挂俩就停摆；判定+交集+余量四读数让配置前有账、
评审有据、容量有预算。

**Verify**：`QuorumConsistencyTest` 5 用例全绿（(3,2,2) 经典 /
(5,3,3) 预算 / (3,1,1) 弱一致 / (4,3,2) 边界 / 畸形五型 fail-fast）。
首测交集负值未钳暴露契约分歧，按「0=无保证」口径修正。

**Status**：done（2026-09-22）
