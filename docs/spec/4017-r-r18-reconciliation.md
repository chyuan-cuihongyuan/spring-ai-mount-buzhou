# Spec 4017 — R 系 R18 周期对账（effort #4017，R18）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6035–T6036，impl 2118）。
> 对账门十八号：Wave 3 五新类型快照补登 + 全仓离线 verify + 台账核账。

## Problem Statement

Wave 3（R13–R17）五件新类型未入公共面快照——全仓 verify 快照门
必红；档案计数需对齐。

## Solution

- 快照补登：1138→1143（ZoneMapPruner/SizeTieredMergePicker/
  BitcaskKeydir/SlabClassPacker/HnswBeamSearch——cleanup×3+cache×1+
  memory×1 三段）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1138→1143；
- 全仓 16 模块离线 `mvn verify`（三门全绿）；
- RSession4000LedgerAuditTest 台账核账（spec 4000–4016 十七轮
  四件套零缺位）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳入 4000–4016）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：18/50=36%。
