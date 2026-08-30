# Wayfinder Map — Buzhou 错误签名 JSONL 导出（effort #74，50 轮自迭代第 39 轮）

> effort #74，延续 #73（T405–T406 / impl-258）。主线：**spec 83 fog 项「签名导出
> （OLAP JSONL 面）」**——错误族只在进程内（top/健康段 5 条），趋势分析（各族
> 随时间的消长）需要全量导出。

## Destination

`ErrorSignaturesJsonl.export(registry, Writer)`（静态面）：全部在册签名一行一
JSON（{signature, count}，count 降序与 top 同序）；Jackson 转义纪律；空表零行；
返回行数。导出五族补齐（观测 span/event、eval run、ab run、错误签名）。

## Notes

- 借鉴：spec 88/94 同族（DuckDB read_json_auto 直装）。

## Decisions so far

- 无时间戳列（进程内表无时间维——按导出时刻落 OLAP 侧时间列即可）。

## Not yet specified

- 导出后清零（窗口化统计——需 reset 面另议）。

## Out of scope

- 沿用 #7–#73。

## Tickets

- [x] [T407 ErrorSignaturesJsonl 静态导出](tickets/T409-signatures-jsonl.md)（impl-259）
- [x] [T408 2 例红队（count 降序逐行 JSON/空表零行）+ 收口](tickets/T410-signatures-jsonl-close.md)
