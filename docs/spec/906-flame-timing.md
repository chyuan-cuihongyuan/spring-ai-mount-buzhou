# 906 — 工具耗时火焰图数据面

> 来源：I 会话第 7 轮 = effort #906（[T1263](../../.wayfinder/tickets/T1263-flame-timing-shape.md) / [T1264](../../.wayfinder/tickets/T1264-flame-timing-verify.md) / impl 659）。借鉴：Brendan Gregg [flamegraph](https://github.com/brendangregg/FlameGraph)（≈23K★）——self/cumulative 时间分解是性能归因的标准读数。

## Problem Statement

`ToolGraphAnalyzer`（spec 519/717）聚合调用计数与错误率，无耗时维度：「哪个工具最耗时」「哪条 agent-as-tool 调用链最贵」不可见。core 的 ToolTimingAggregator（spec 700）是进程内热路径计数器（健康段 top-N），observability 的 span 历史是离线全量分析面——补 self/cumulative 分解后即可回答归因问题（火焰图数据面，非渲染）。

## 目标

- `ToolGraphAnalyzer.timings(List<SpanRecord>)` 新纯函数（公开 API，同 analyze/cycles 纪律：零 IO、入参 null fail-fast）：
  - TOOL span 过滤（kind 忽略大小写）；按 `parentSpanId` 在 TOOL 子集内建树（父不在集合的 span 视为根；parent 指针环用 visited 防护——数据损坏不死循环）；
  - span 耗时 = `endedAt − startedAt`（endedAt null 的 RUNNING 中间态计 0——诚实不估）；
  - 每工具聚合：`calls` / `totalSelfMs`（Σ 自身耗时）/ `totalCumulativeMs`（Σ 自身+子孙 TOOL 耗时）；
  - 输出公共 record `ToolTimingProfile(String tool, long calls, long totalSelfMs, long totalCumulativeMs)`，按 totalSelfMs 降序 + tool 字典序 tie-break（稳定确定性）；
- 既有 `ToolTotal` / `analyze` / `cycles` 零变化。

## 兼容性

纯增量：公共类新增静态方法 + 新公共嵌套 record，零既有行为变化。
