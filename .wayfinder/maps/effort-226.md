# Wayfinder Map — Buzhou spec 文档覆盖门（effort #226，B 会话第 49 轮）

> B 会话第 49 轮。spec 文件与 README 表靠人工同步——新 spec 忘记入表/链接
> 拼错，用户可见面静默缺能力。治理轮惯例（绑定矩阵先例）：把纪律变成测试。

## Destination

SpecCoverageTest（starter 测试）：docs/spec/*.md 每个文件被 README 引用
（按文件名匹配）；README spec 链接全部指向实存文件。双向完整性门——新 spec
不入表即红、死链即红。

## Notes

- 号段：B=奇数 spec（本轮 213）；轮次 .wayfinder200+。
- 先例：ConfigBindingsMatrixTest（新键不入矩阵即红）同款治理思想。

## Decisions so far

- 双向断言（spec→README 引用 + README→spec 实存）。

## Not yet specified

- 无。

## Out of scope

- 沿用各轮；runbook 覆盖门。

## Tickets

- [x] [T587 SpecCoverageTest 双向完整性门](../tickets/T587-coverage.md)（impl-321）
- [x] [T588 门自检（当前库全绿）+ 例外清单机制](../tickets/T588-coverage-close.md)（impl-321）
