# Spec 6035 — T 系 T36 周期对账（effort #6035，T36）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6271–T6272，impl 2236）。
> 对账门六十六号（T 系第六波）：Wave 6 快照补登 + 全仓 verify
> + 台账核账。

## Problem Statement

Wave 6（T31–T35）五个新公共类型未入公共面快照——全仓
verify 快照门必红；第六波需对账封账。

## Solution

- 快照补登：1233→1238（BuddyAllocator——memory +
  ExternalMergeSort——fs + ExtendibleHashing——metrics +
  ElevatorScan——policy + ArenaAllocator——memory）；
- api-surface.md 同步 +6 行（含 5 类型行）；CONTEXT 计数
  1233→1238；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- TSession6000LedgerAuditTest 台账核账（spec 6000–6034
  零缺位）。

## User Stories

1. 作为对账审计者，T 系第六波工件链四面互证全绿。
2. 作为后续轮作者，快照门恢复绿——继续推进。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳
  6000–6034）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：T36/50（72%）。
