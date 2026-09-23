# Spec 4047 — R 系 R48 周期对账（effort #4047，R48）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6095–T6096，impl 2148）。
> 对账门四十八号：Wave 8 五新类型快照补登 + 全仓 verify + 台账核账。

## Problem Statement

Wave 8（R43–R47）五件新类型未入公共面快照——全仓 verify
快照门必红；档案计数需对齐。

## Solution

- 快照补登：1163→1168（ThreeWayMerge/CommitGraph/RopeBuffer/
  SchemaEvolution/BanEscalation——policy×5 一包）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1163→1168；
- 全仓 16 模块 `mvn verify`（三门全绿）；
- RSession4000LedgerAuditTest 台账核账（spec 4000–4046
  四十轮四件套零缺位）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳入 4000–4046）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：48/50=96%。
