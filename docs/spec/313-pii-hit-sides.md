# Spec 313 — PII 命中分侧（effort #313）

> wayfinder map：`.wayfinder/maps/effort-313.md`（T617–T618）。借鉴：Presidio
> anonymizer 统计口径（spec 144 原始来源分侧深化——fog 152 项）。

## Problem Statement

PII 命中统计只记总量：用户输入里的 PII（预防提示面）与工具输出里的 PII
（脱敏规则面）混在一列——两类策略调优看不到各自的事实分布。

## Solution

`PiiHitStats.Side{INPUT, OUTPUT, UNSPECIFIED}` 维度：

- `record(type, side)` / `recordCustom(name, side)` 双参重载（旧单参 =
  UNSPECIFIED 兜底，兼容未升级宿主）；总量口径不变。
- 双钩接线：`PiiInputRedactionHook` → INPUT；`PiiRedactionHook` → OUTPUT。
- 查询：`topBySide(side, n)`（count 降序+名字典序稳定序）、
  `countOf(name, side)`。
- JSONL 导出行补 `inputCount` / `outputCount` 两列（DuckDB/ClickHouse
  分侧时序分析）；`count` 总量列保留（既有报表零破坏）。

## User Stories

1. 作为合规工程师，分侧列直接回答「EMAIL 命中 70% 来自工具输出」——
   脱敏规则优先级有据。
2. 作为宿主，旧打点代码不升级也不断（UNSPECIFIED 兜底）。

## Implementation Decisions

- 侧维度与名维度共用封顶（自定义名 64 折 overflow——侧不独立扩表）。

## Testing Decisions

- `PiiHitStatsSideTest`：分侧计数/排行稳定序/单参兼容 UNSPECIFIED/
  JSONL 三列断言/overflow 与分侧共存。

## Out of Scope

- 会话级分组；侧级独立封顶。

## Further Notes

- PII 面族：检测器（51 系）/ 自定义规则 yml（129）/ 事件脱敏（177）/
  命中统计（144）/ 输入侧（164）/ 导出（166）/ **分侧（本轮）**。
