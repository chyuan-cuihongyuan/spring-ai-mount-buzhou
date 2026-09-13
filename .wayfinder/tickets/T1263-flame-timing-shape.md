---
id: T1263
title: 工具耗时火焰图数据面的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-13
---

## Question

I 会话第 7 轮：ToolGraphAnalyzer（spec 519/717）只有调用计数与错误率——per-tool 耗时维度（flamegraph 的 self/cumulative 思想）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 7 轮 = effort #906 / spec 906 / impl 659）：缺口成立——哪个工具最耗时（self）vs 哪条调用链最贵（cumulative）当前不可见；core 的 ToolTimingAggregator（spec 700）是**进程内热路径**聚合，observability 的 span 历史是**离线全量**分析面，二者互补。落点 buzhou-observability 新公共纯函数 `ToolGraphAnalyzer.timings(List<SpanRecord>)`：TOOL span 过滤（kind 忽略大小写同 analyze 口径）→ 按 parentSpanId 在 TOOL 子集内建树（父不在集合=根；环防护 visited；endedAt null 的 RUNNING span 计 0 诚实不估）→ 每工具聚合 calls / totalSelfMs（自身）/ totalCumulativeMs（自身+子孙）→ 按总 self 降序 + 字典序 tie-break 稳定输出（火焰图宽板块在前直觉）。新公共 record `ToolTimingProfile`，不动既有 ToolTotal（零破坏）。
