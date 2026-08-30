# Wayfinder Map — Buzhou 导出清单（effort #118，A 会话第 13 轮）

> A 侧编号策略沿用 #111 声明。借鉴 git pack 索引（目录与数据体分立）。

## Destination

观测导出的 manifest：一行一会话六列（id/首末活动/轮次/span/event 计数）——
「这份 JSONL 里有什么」不必解析数据体即可审计与核对；eventCount 现算与
数据体互核正是清单用途。

## Notes

- 行序随 store 摘要序（不承诺排序——消费方按 id 定位）；列序稳定（按位
  消费契约）；空库诚实零行。

## Decisions so far

- [exportManifest](tickets/T471-export-manifest.md) — 复用既有分页游标与
  JSONL 行管线；返回会话数。

## Not yet specified

- 尾采样/增量路径的 manifest 变体（与 exportAllSampled 水位组合）。

## Out of scope

- 校验和/指纹列（数据体逐字节校验）；二进制索引。

## Tickets

- [x] [T471 导出清单](tickets/T471-export-manifest.md)（impl-285）
- [x] [T472 收口提交](tickets/T472-export-manifest-close.md)（impl-285）
