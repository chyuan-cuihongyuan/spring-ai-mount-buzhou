# Spec 9000 — W 会话 9000 系对账门落位（effort #9000，W1）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9001–W9002，impl 2353）。
> 借鉴：V 系对账门（spec 8000）公式族第十应用——预防式台账纪律变测试。

## Problem Statement

50 轮自迭代的工件链（spec ↔ 票对 ↔ impl ↔ README 行）四面手工
维护，漏登静默——对账缺口发现滞后到收口轮才暴露的病。

## Solution

WSession9000LedgerAuditTest（starter 测试）：扫 docs/spec 9000–9049
号段驱动，公式互证——spec N ↔ shape 票 W(9001+2(N−9000))/verify=+1
↔ impl 2353+(N−9000) ↔ README 覆盖；起点 9000 + 严格递增。
范围自扩展：后续轮落地自动纳入。

## Testing Decisions

- 四测各锚一面（票对/impl/README/号段序）；spec 9000 自证（门自身
  的 spec 文件即最小对账集）。

## Out of Scope

- 不做跨系对账（V 系门各自守段）；不做内容语义校验（号段存在性互证）。

## Further Notes

- 与 VSession8000LedgerAuditTest（spec 8000）同族不同面：基点
  8000/8001/2302 vs 9000/9001/2353。
