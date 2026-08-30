# Spec 109 — 观测导出 gzip 面（effort #71）

> wayfinder map：`.wayfinder71/MAP.md`（T401–T402）。spec 88 fog 项（gzip）。

## Problem Statement

观测 JSONL 导出（spec 60/67）明文落盘/跨网：JSONL 高重复结构下 gzip 可省一个
量级体积——对象存储归档与跨环境搬运的默认姿势缺席。

## Solution

`ObservabilityJsonlExporter.exportAllGzip(OutputStream)` 与
`exportAllSinceGzip(OutputStream, Instant)`：GZIPOutputStream 内包 UTF-8 Writer，
复用既有导出管线——解压内容与未压缩导出逐字节一致；水位/计数/at-least-once 语义
不变；调用方负责外层流生命周期（压缩完整性由 writer close 收尾）。

## User Stories

1. 作为数据作者，我要归档体积降一个量级，所以存储成本可控。
2. 作为红队，我要解压后与明文导出逐字节一致，所以压缩不引入语义漂移。

## Implementation Decisions

- 独立重载不加参数（Writer 面二进制兼容）。

## Testing Decisions

- gzip roundtrip 与明文逐字节等值 + 计数一致 + 压缩收益断言；
  since 变体水位语义（EPOCH 全量）。

## Out of Scope

- eval/AB 导出器同构 gzip；分卷策略（按日分卷归调用方）。

## Further Notes

- OLAP 装载端：DuckDB `read_json_auto(gzip_decompress(...))` 同族可直接吃。
