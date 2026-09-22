# impl 1519 — BoundedStaleness 有界旧读（R119 = effort #1918 / spec 1918 / T3037-T3038）

**What**：`BoundedStaleness`（core/transaction 静态纯函数 + Staleness
枚举）——stalenessMillis（read−lastWrite 钳 0 偏斜诚实归零）+
verdict 两态（≤ bound WITHIN_BOUND 边界含上/STALE）；时点/界非负
fail-fast。

**Why**：Cosmos DB bounded staleness 档——「旧不超过 X 毫秒」是
强一致与最终一致之间可售的合同；单次读的旧度判定让副本读合法
与否有据。与 QuorumConsistency 配置账互补。

**Verify**：`BoundedStalenessTest` 4 用例全绿（旧度读数/判定两态
含上/偏斜钳 0/畸形两型 fail-fast）。

**Status**：done（2026-09-23）
