# 1414 — 会话 Span 树拓扑读面

> 来源：L 会话第 15 轮 = effort #1414（票 T2129 / T2130 / impl 1067）。**换题记录**：原题 R44 导出分块勘察发现 RollingJsonlWriter 已有 rotations/rotationFailures+压缩代际（spec 712）——换入 R39 题的 core 侧通用化。借鉴：Jaeger DAG 依赖图（结构形状独立于时延维）。

## Problem Statement

Span 集合的时延维已有分析面（TurnLatencyPercentiles/ToolGraphAnalyzer 火焰图 timings），**结构形状**维全无：这轮嵌套多深（工具递归循环失控）、单节点扇出多大（并行工具风暴）、span kind 分布如何——时延直方看不出来。

## 目标

- `SpanTreeTopology`（core/observability，纯函数静态面，private 构造）：
  - `analyze(List<SpanRecord>)` → `record Topology(totalSpans, rootCount, orphanCount, maxDepth, maxFanout, kindHistogram)`；
  - 深度 = 根→叶最长节点数（根=1）；扇出 = 单节点最大子数；
  - 孤儿（父引用不在集合内——导出截断的正常形态）与 SpanParentIntegrityAudit 同判，只计数不列明细（明细归彼）；
  - **环防护**：父引用成环按 visited 收敛（环成员不计深、不炸栈——诚实入档）；
  - kind 直方数量降序、平名典序。
- 无序容忍、空集合零拓扑哨兵。

## 兼容性

纯函数零 IO；不触 store/hook；只读不裁决。

## Out of Scope

- 时延归因（Timings 族已有）。
- 孤儿明细（SpanParentIntegrityAudit 已有）。
- 跨会话聚合（analyze 口径为单 span 集合，调用方自由组合）。
