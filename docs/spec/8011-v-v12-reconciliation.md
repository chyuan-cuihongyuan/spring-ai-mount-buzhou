# Spec 8011 — V 系 V12 周期对账（effort #8011，V12）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8023–V8024，impl 2313）。
> 对账轮（Wave 2 收口——12/50=24%）。

## Scope

- 快照批补登 +7（1292→1299：DinicMaxFlow/BipartiteChecker/
  TreeDiameter/GraphColoring——concurrent + HungarianMatcher
  ——policy + HopscotchHashTable——cache + PerfectHash——metrics
  （V13/V14 已先行提交件并入本轮批补登））；
- api-surface.md 同步 +5 行 + CONTEXT 计数同步（1299×13）；
- 全仓 16 模块 verify 三门绿（R48 协议口径）+ V 系第二波
  对账门（spec 8000–8010 零缺位）+ 对账轮尝试 push。

## Out of Scope

- Wave 3（哈希过滤族）不在本轮。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿；全仓
  verify BUILD SUCCESS。

## Further Notes

- 里程碑：V12/50（24%）。
