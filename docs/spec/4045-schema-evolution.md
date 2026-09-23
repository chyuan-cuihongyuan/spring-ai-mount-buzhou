# Spec 4045 — Avro 读写模式解析（effort #4045，R46）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6091–T6092，impl 2146）。
> 借鉴：Apache Avro Schema Resolution（读写模式演化解析规则）。

## Problem Statement

结构化数据跨版本读写的病：无模式演化规则（写方加字段读方
炸、类型加宽即崩）或自由漂移（语义静默劣化）——**确定性
解析计划面**缺失。

## Solution

`SchemaEvolution`（core/policy）：

- 字段配对：按名匹配（含 alias 别名表）；
- 类型加宽白名单：int→long/float/double、long→float/double、
  float→double（Avro widening 同表）；同型直读；其余组合
  fail-fast（解析失败带字段名）；
- 读方缺字段 → 读方默认值补位（usesDefault 显形）；无默认
  fail-fast；
- 写方多余字段 → dropped 读数（数据被诚实丢弃可见）；
- `Plan` 结构化输出：逐字段（读/写型、是否加宽、是否默认补位）
  + dropped 清单——确定性可回放；
- fail-fast：null schema、空名。

## User Stories

1. 作为会话快照作者，旧快照新代码可读（加宽兼容、默认补位）。
2. 作为审计作者，同对 schema 同解析计划（确定性可回放）。

## Testing Decisions

- 同型直读；三条加宽链各证；不兼容（string→int）fail-fast
  带字段名；读方缺字段默认补位 vs 无默认 fail-fast；alias
  配对；写方多余字段 dropped 显形；确定性回放。

## Out of Scope

- 不做 union/enum/fixed/array/map 类型解析（record+原语口径）；
  不做 schema JSON 文本解析（结构化模型直入）；不做编码器
 （本件是解析计划面非序列化面）。

## Further Notes

- 与 SessionExport（会话导出移植）互补：导出格式跨环境 vs
  字段级跨版本。Wave 8 第四件。
- 里程碑：46/50。
