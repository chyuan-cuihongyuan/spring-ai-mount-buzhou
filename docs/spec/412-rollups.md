# Spec 412 — 时间桶预聚合（effort #412）

> wayfinder map：`.wayfinder/maps/effort-412.md`（T715–T716）。D 会话第 13 轮。

## Problem Statement

dashboard 统计全是点态（per-session/tool/model 聚合），无时间轴——「每小时
token 消耗/错误趋势」这类最常用运营图表无数据面；原始 span 导出后离线
聚合是唯一路径。

## Solution

`DashboardQueryService.rollups(from, to, bucket)`（M3 fixed-window
downsampling 借鉴）：

- **枚举**：listSessionSummaries 翻页全会话 → spansOfSession 逐会话；
  过滤 kind ∈ {TURN, MODEL_CALL, TOOL_CALL} 且 startedAt ∈ [from, to)。
- **分桶**：epoch 对齐 floor（`startedAt - epoch mod bucket`）——跨实例
  对齐口径一致；桶粒度 Duration（1m..1d 常用）。
- **`TimeBucket`**：{start, turns（TURN 数）, modelCalls, toolCalls,
  errors（status=ERROR 三类合计）, promptTokens, completionTokens}。
- **空桶补齐**：from→to 区间内无数据的桶也返回零值桶（图表连续性——
  Prometheus rate 需连续桶）；升序。
- **上界**：桶数 > 1000 IllegalArgumentException（payload 纪律）。
- HTTP：`GET {prefix}/api/rollups?from=…&to=…&bucket=5m`（from/to
  ISO-8601；bucket 支持 ns/ms/s/m/h/d 后缀简写与 ISO-PT 形态）。
- 只读查询——零行为变化。

## User Stories

1. 作为运维，我想小时级 token/错误趋势一查询即得，so 不用导出 span
   离线聚合。
2. 作为图表作者，我想空桶补齐，so 时间轴连续无锯齿假象。
3. 作为宿主，我想桶数有上界保护，so 误查（秒级×月窗）不会打爆面板。

## Implementation Decisions

- 查询时聚合（不物化）——dashboard 规模够用；物化为扩散候选。
- errors = 三类 span 的 ERROR 合计（不细分维度——细分另议）。

## Testing Decisions

- 跨会话聚合数值（turns/modelCalls/toolCalls/tokens/errors）正确；
- 空桶补齐 + 升序 + 桶对齐（epoch 对齐验证）；
- 桶数上界 fail-fast；
- HTTP 参数解析（简写与 ISO 形态、坏参数 4xx 路径）。

## Out of Scope

- 预聚合物化；per-model 桶；P99 直方图；流式游标。

## Further Notes

- 新公共类型嵌套 `DashboardQueryService.TimeBucket`（外类已在快照）——
  快照零 diff 预判仍 regenerate 验证。
