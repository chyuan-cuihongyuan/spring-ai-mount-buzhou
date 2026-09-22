# impl 1507 — ShardSkewAudit 分片偏斜审计（R107 = effort #1906 / spec 1906 / T3013-T3014）

**What**：`ShardSkewAudit`（core/policy 静态纯函数）——skewRatio
（max/avg 偏斜比）+ needsReshard（比率 ≥ 阈值触发建议）；loads
非空非负且 avg>0、阈值≥1 fail-fast。

**Why**：Spark data skew 语义——总体吞吐掩盖热分片双倍承担；
max/avg 偏斜比让重分片时机主动有刻度。与反热点重平衡（迁移动作）
互补：这是「该不该动」的判定。

**Verify**：`ShardSkewAuditTest` 4 用例全绿（偏斜比精确/绝对均衡
1.0/阈值两侧/畸形四型 fail-fast）。

**Status**：done（2026-09-23）
