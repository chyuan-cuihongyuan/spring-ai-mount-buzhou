# impl 1482 — ErasureCodingBudget 纠删码冗余预算（R82 = effort #1881 / spec 1881 / T2963-T2964）

**What**：`ErasureCodingBudget`（core/policy 静态纯函数）——
usableRatio（k/(k+m)）+ tolerableFailures（=m）+ repairReads（=k
修复读放大）+ totalShards（k+m）；data≥1/parity≥1（零冗余非法）
fail-fast。

**Why**：MinIO/Ceph EC(k,m) 语义——可用率/容忍度/修复读三读数分散
在文档里，评审缺统一口径；EC(4,2) vs 三副本同容忍省 1/3 容量、
修复读放大 k 倍的账面化。落轮 grep 复核无占坑。

**Verify**：`ErasureCodingBudgetTest` 4 用例全绿（EC(4,2) 四读数/
EC(8,4) 修复读翻倍/EC(1,2) 副本对照/畸形三型 fail-fast）。

**Status**：done（2026-09-23）
