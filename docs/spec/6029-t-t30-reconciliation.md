# Spec 6029 — T 系 T30 周期对账（effort #6029，T30）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6259–T6260，impl 2230）。
> 对账门六十五号（T 系第五波）：Wave 5 快照补登 + 全仓 verify
> + 台账核账。

## Problem Statement

Wave 5（T25–T29）五个新公共类型未入公共面快照——全仓
verify 快照门必红；第五波需对账封账。

## Solution

- 快照补登：1228→1233（MpscQueue/StripedLock/IndexedHeap/
  StrideScheduler/Mlfq——concurrent×5）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1228→1233；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- TSession6000LedgerAuditTest 台账核账（spec 6000–6028
  零缺位）。

## User Stories

1. 作为对账审计者，T 系第五波工件链四面互证全绿。
2. 作为后续轮作者，快照门恢复绿——继续推进。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳
  6000–6028）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：T30/50（60%）。
