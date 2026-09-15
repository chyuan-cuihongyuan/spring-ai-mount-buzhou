# impl 1408 — ViolationEpisodeMerger 追限会话化（R8 = effort #1807 / spec 1807 / T2815-T2816）

**What**：`ViolationEpisodeMerger`（core/ratelimit 静态纯函数）——`merge(gap,
points)` 时点排序后相邻差 ≤ gap 并入同事件段（Episode start/end/hits/span，
单点成段）→ MergeReport（episodes/totalHits/longest/mergedSpans +
hitsPerEpisode -1 哨兵）；乱序容忍、负 gap/null 时点 fail-fast。

**Why**：Prometheus 告警分组 / GA session gap 会话化思想——10 次散点追限
（偶发抖动，加余量）与 1 段 10 连击（系统性超载，扩容/降级）处置相反，
段数/最长段/平均密度把两者分开读。

**Verify**：`ViolationEpisodeMergerTest` 5 用例全绿。

**Status**：done（2026-09-16）
