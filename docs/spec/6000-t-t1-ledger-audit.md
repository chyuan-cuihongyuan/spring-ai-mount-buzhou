# Spec 6000 — T 系 T1 对账门落位（effort #6000，T1）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6201–T6202，impl 2201）。
> 对账门六十零号（T 系开卷）：SSession5000LedgerAuditTest 公式族第七应用。

## Problem Statement

T 会话（6000 系）开卷无对账门——后续轮工件链（spec/票/impl/
README）无四面互证，号段漂移无法在 CI 拦截。

## Solution

`TSession6000LedgerAuditTest`（starter）：

- 四面互证：spec 文件（6000–6049 数值号段自扩展扫描）↔
  shape/verify 票对（T6201+2(N−6000)）↔ impl 切片
 （2201+N−6000）↔ README 纵深行；
- 号段严格递增断言（6000 起跑）；
- 范围自扩展：后续轮落档自动纳入对账，T6k 全量核账。

## User Stories

1. 作为对账审计者，T 系工件链缺位在 CI 必红。
2. 作为后续轮作者，号段公式即法律，占坑无需人工核对。

## Testing Decisions

- 对账门自跑：spec 6000（本轮）四件套齐整即绿；公式族与
  SSession5000LedgerAuditTest 同构（第七应用）。

## Out of Scope

- 不做跨会话号段互查（各系门各自为政）。

## Further Notes

- 里程碑：T1/50（2%）。S 系已收口交接：6000/2201/T6201。
