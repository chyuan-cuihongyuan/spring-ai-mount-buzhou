# Wayfinder Map — Buzhou 观测导出 gzip 面（effort #71，50 轮自迭代第 36 轮）

> effort #71，延续 #70（T397–T398 / impl-255）。主线：spec 88 fog「gzip」——
> 观测 JSONL 明文导出跨环境搬运（对象存储归档/跨网传输）体积大；JSONL 高重复
> 结构 gzip 收益一个量级。

## Destination

`ObservabilityJsonlExporter.exportAllGzip(OutputStream)` / `exportAllSinceGzip(
OutputStream, Instant)`：GZIP 流内 UTF-8 Writer 复用既有导出管线——解压内容与
未压缩面逐字节一致；水位/计数语义不变；调用方负责外层流。

## Notes

- 借鉴：JSONL 归档惯例（gzip + 按日分卷——分卷策略归调用方）。

## Decisions so far

- 重载不加参数（独立方法——Writer 面零变化，二进制兼容）。

## Not yet specified

- eval/AB 导出器同构 gzip（需求证据后议）；分卷策略。

## Out of scope

- 沿用 #7–#70。

## Tickets

- [x] [T401 gzip 双重载（全量+水位）](../tickets/T403-gzip-export.md)（impl-256）
- [x] [T402 2 例红队（roundtrip 逐字节等值/水位语义）+ 收口](../tickets/T404-gzip-close.md)
