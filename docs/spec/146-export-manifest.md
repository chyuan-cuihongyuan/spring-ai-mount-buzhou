# Spec 146 — 导出清单（effort #118）

> wayfinder map：`.wayfinder118/MAP.md`（T471–T472）。借鉴：git pack 索引
> （pack 与 idx 分立——内容审计先看目录再按需下钻）。

## Problem Statement

观测 JSONL 导出件只有一个数据体：想知道「这份文件里有哪些会话、各多少
span/event」必须全量解析；工单附件与归档核对场景要的是目录页。

## Solution

`ObservabilityJsonlExporter.exportManifest(Writer)`：一行一会话的 manifest
JSONL——sessionId / firstActivityAt / lastActivityAt / turnCount / spanCount /
eventCount 六列。列序稳定（下游按位消费契约）；行序随 store 摘要序（不承诺
排序，消费方按 id 定位）；eventCount 现算（Summary 无此列——清单与数据体
互核正是用途）。返回会话数；空库诚实零行。

## User Stories

1. 作为审计员，我拿 manifest 即可核对导出件的会话清单与计数，不必解析整个
   数据体。
2. 作为数据工程，eventCount 与数据体行数互核，所以导出管线的丢行/重行即刻
   可见。

## Testing Decisions

- 红队：多会话六列与数据体计数互核（按 id 定位不按位——行序无承诺）；空库
  零行；列序稳定断言。

## Out of Scope

- 校验和列；尾采样/增量变体 manifest；二进制索引。

## Further Notes

- 与 spec 136（尾采样）组合：sampled 导出附 manifest 即得「留了谁」目录。
