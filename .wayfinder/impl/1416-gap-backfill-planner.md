# impl 1416 — GapBackfillPlanner 缺口回填计划器（R16 = effort #1815 / spec 1815 / T2831-T2832）

**What**：`GapBackfillPlanner`（core/recovery 静态纯函数）——plan(from, to,
present) 排序去重后扫缺口 → BackfillPlan（缺口闭区间清单含首尾 +
largestGapSpan + missingRatio(-1 哨兵) + complete()）；空区间 from=to+1；
区间倒挂/越界/null 元素 fail-fast。

**Why**：Kafka offset 补填 / Prometheus backfill 思想——重放不重不漏的前提
是先知道缺哪段；缺口合并为区间让回填可分批可并行，最大缺口直读重放
瓶颈。

**Verify**：`GapBackfillPlannerTest` 5 用例全绿。

**Status**：done（2026-09-16）
