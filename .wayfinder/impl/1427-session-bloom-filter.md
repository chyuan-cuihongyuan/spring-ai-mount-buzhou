# impl 1427 — SessionBloomFilter 会话布隆粗筛（R27 = effort #1826 / spec 1826 / T2853-T2854）

**What**：`SessionBloomFilter`（core/session，synchronized 小临界区）——
add/mightContain（零假阴性契约）+ 确定性哈希（seed 混合扩散）+ fillRatio
饱和度（SATURATION_THRESHOLD=0.5 重建建议）；4096×3 默认；畸形 fail-fast。

**Why**：Bloom filter 思想——新会话「一定没见过」的最常见分支只需位图级
粗筛（512B 覆盖万级会话），零索引查询直达新建路径；确定性哈希可回放。

**Verify**：`SessionBloomFilterTest` 5 用例全绿（含千探针误报界）。

**Status**：done（2026-09-16）
