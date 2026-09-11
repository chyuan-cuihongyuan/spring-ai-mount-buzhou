# Spec 527 — 数据集 CSV 互操作（effort #527）

> wayfinder map：`.wayfinder/maps/effort-527.md`（T807–T808）。E 会话第 28 轮。

## Problem Statement

数据集只经 API 逐项添加/轨迹回流——与表格工具的 CSV 双向搬运空白
（批量整理场景每次都要写胶水代码）。

## Solution

`eval.EvalDatasetCsv`（纯函数，RFC 4180）：

- toCsv(items)：表头 `input,expected` + 转义行（含逗号/引号/换行的字段
  自动引号包裹、双引号转义）。
- fromCsv(csv)：状态机解析（引号域内逗号/换行/CRLF 兼容）、表头宽松校验
  （大小写/空白容忍）、空行容忍、候选项 id 空（同手工项管道）。
- export(Writer, items) → 行数（导出族同构）。

## User Stories

1. 作为评测方，我想把 CSV 表格直接变成评估数据集（也能导回去）， so
   批量整理在表格工具完成、框架零胶水。

## Implementation Decisions

- 只搬运 input/expected 两列（最小互操作面）；解析纯内存（数据集规模
  有界——txt 万行为主）。

## Testing Decisions

- 往返保持逗号/引号/多行字段；表头严格但大小写空白宽松；空行容忍；
  空 CSV 拒绝；导出行数。

## Out of Scope

- 溯源列；流式；方言。

## Further Notes

- 新公共类型 `EvalDatasetCsv` 随轮 regenerate 快照 + api-surface.md 加行。
