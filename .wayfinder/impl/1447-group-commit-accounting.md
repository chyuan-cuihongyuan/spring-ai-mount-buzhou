# impl 1447 — GroupCommitAccounting 组提交账面（R47 = effort #1846 / spec 1846 / T2893-T2894）

**What**：`GroupCommitAccounting`（core/fs 纯 record）——Account 契约构造
（0≤flushes≤writes、有写必有刷）+ amortizationRatio/savedFlushes/
savingsNanos（可为负）/savingsRatio（哨兵 -1）；畸形四型 fail-fast。

**Why**：MySQL group commit/PostgreSQL commit_delay 思想——合并刷盘摊薄
fsync 但有等批迟滞；摊薄倍数/省刷数/净省三读数让组提交开不开不拍脑袋，
净省为负面诚实可判。

**Verify**：`GroupCommitAccountingTest` 4 用例全绿。

**Status**：done（2026-09-16）
