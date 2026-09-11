# Spec 519 — 工具调用图谱统计（effort #519）

> wayfinder map：`.wayfinder/maps/effort-519.md`（T789–T790）。E 会话第 20 轮。

## Problem Statement

TOOL span 只被桶聚合计数（412），调用序与 per-tool 成败分布无读数面：
「哪些工具总被连着用」「哪个工具错误集中」无数据依据。

## Solution

`observability.analytics.ToolGraphAnalyzer`（纯函数）：

- 输入 spans → 过滤 kind=TOOL → 按 (sessionId, turnSeq, startedAt) 排序
  → 同轮相邻对生成有向边 `toolA→toolB` 计数；per-tool calls/errors
  （status 含 ERROR 判红）+错误率。
- `ToolGraphReport(edges[Edge(from,to,count)], tools[ToolTotal(tool,
  calls, errors, errorRate)])`——edges 按 count 降序、tools 按 calls 降序
  （输出稳定：同值字典序）。
- 便捷重载 `analyze(ObservabilityStore, sessionId)`（spansOfSession 单
  会话读）；跨会话聚合归 OLAP 下游。

## User Stories

1. 作为 prompt 工程师，我想看同轮工具先后链统计， so 高频相连的工具对
   可以合并或组合（工具面优化有依据）。
2. 作为运维，我想看 per-tool 错误率排行， so 错误集中的工具优先接熔断
   （131）或限流。

## Implementation Decisions

- 跨轮不连边（会话内时序链不做跨轮假设——诚实边界）。
- status 判红大小写不敏感（ERROR/error 均红——供应商差异容忍）。

## Testing Decisions

- 三工具同轮顺序链 → 两条边各 1；同轮乱序输入（startedAt 排序）边同；
  错误 span 计入工具 errors；非 TOOL（TURN/MODEL）忽略；跨轮不连边。

## Out of Scope

- 跨会话聚合；JSONL 导出；图可视化。

## Further Notes

- 新公共类型 `ToolGraphAnalyzer`（嵌套 `ToolGraphReport`/`Edge`/
  `ToolTotal`）随轮 regenerate 快照 + api-surface.md 加行。
