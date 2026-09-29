# Spec 8035 — V 系 V36 周期对账（effort #8035，V36）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8071–V8072，impl 2337）。
> 对账轮（Wave 6 收口——36/50=72%）。

## Scope

- 快照批补登 +5（1312→1317：KMeansClustering/DbScanClusterer/
  PowerIteration/ViterbiDecoder——eval + JaroWinklerSimilarity
  ——metrics）；
- api-surface.md 同步 +5 行 + CONTEXT 计数同步（1317×13）；
- 全仓 verify 三门绿（R48 协议口径）+ V 系第六波对账门
  （spec 8000–8034 零缺位）+ 对账轮尝试 push。

## Out of Scope

- Wave 7（加密信任族）不在本轮。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿；全仓
  verify BUILD SUCCESS。

## Further Notes

- 里程碑：V36/50（72%）。
