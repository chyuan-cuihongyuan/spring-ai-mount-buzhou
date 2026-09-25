# Spec 6005 — T 系 T6 周期对账（effort #6005，T6）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6211–T6212，impl 2206）。
> 对账门六十一步号（T 系第一波）：Wave 1 快照补登 + 全仓 verify
> + 台账核账。

## Problem Statement

Wave 1（T2–T5）四个新公共类型未入公共面快照——全仓 verify
快照门必红；首波需对账封账。

## Solution

- 快照补登：1209→1213（SplayTree/Treap——concurrent，
  SparseTable/MonotonicDeque——metrics）；
- api-surface.md 同步 +4 行；CONTEXT 计数 1209→1213；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- TSession6000LedgerAuditTest 台账核账（spec 6000–6003
  零缺位）。

## User Stories

1. 作为对账审计者，T 系首波工件链四面互证全绿。
2. 作为后续轮作者，快照门恢复绿——继续推进。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳
  6000–6003）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：T6/50（12%）。SplayTree 删除根锚勘误入档：删除后
  根=前驱仅当左子树非空，否则为右子树根（测试按精确语义
  钉住）。
