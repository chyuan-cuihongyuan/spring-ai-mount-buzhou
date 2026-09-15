# impl 1440 — AntiEntropyDivergence 反熵分歧账（R40 = effort #1839 / spec 1839 / T2879-T2880）

**What**：`AntiEntropyDivergence`（core/recovery 静态纯函数）——compare
键×版本四桶（onlyInPrimary/onlyInReplica/versionMismatch/matched）+
repairWorkload 三型合计 + matchedRatio（-1 哨兵）；null 任一按空表。

**Why**：Cassandra/Dynamo anti-entropy repair 思想——副本健康=数据一致；
三型分歧（丢写/被清/冲突）修复动作不同，分开数才排得了修复批优先级。

**Verify**：`AntiEntropyDivergenceTest` 3 用例全绿。

**Status**：done（2026-09-16）
