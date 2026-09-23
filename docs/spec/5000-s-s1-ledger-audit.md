# Spec 5000 — S 系 S1 对账门落位（effort #5000，S1）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6101–T6102，impl 2151）。
> 对账门五十零号（S 系开卷）：RSession4000LedgerAuditTest 公式族第六应用。

## Problem Statement

S 会话（5000 系）开卷无对账门——后续轮工件链（spec/票/impl/
README）无四面互证，号段漂移无法在 CI 拦截。

## Solution

`SSession5000LedgerAuditTest`（starter）：

- 四面互证：spec 文件（5000–5049 数值号段自扩展扫描）↔
  shape/verify 票对（T6101+2(N−5000)）↔ impl 切片
 （2151+N−5000）↔ README 纵深行；
- 号段严格递增断言（5000 起跑）；
- 范围自扩展：后续轮落档自动纳入对账，S6k 全量核账。

## User Stories

1. 作为对账审计者，S 系工件链缺位在 CI 必红。
2. 作为后续轮作者，号段公式即法律，占坑无需人工核对。

## Testing Decisions

- 对账门自跑：spec 5000（本轮）四件套齐整即绿；公式族与
  RSession4000LedgerAuditTest 同构（第六应用）。

## Out of Scope

- 不做跨会话号段互查（各系门各自为政）。

## Further Notes

- 里程碑：S1/50（2%）。R 系已收口交接：5000/2151/T6101。
