# impl 1325 — RetrievalRankStats 检索命中排名读面（R26 = effort #1725 / spec 1725 / T2651-T2652）

**What**：record(rank)+MRR 千分位累加+Hit@1/@3+miss 分离
**Why**：ES rank_eval/MRR——最终被用条目的排名质量
**Verify**：RetrievalRankStatsTest 3 断言 全绿。 **Status**：done（2026-09-15）
