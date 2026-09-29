# Spec 8005 — V 系 V6 周期对账（effort #8005，V6）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8011–V8012，impl 2307）。
> 对账轮（Wave 1 收口——6/50=12%）。

## Scope

- 快照批补登 +4（1288→1292：BoyerMooreSearch / BitapSearch /
  LevenshteinAutomaton / GlobMatcher——metrics）；
- api-surface.md 同步 +4 行 + CONTEXT 计数同步（1292×13）；
- 全仓 16 模块 verify 三门绿（R48 协议口径）+ V 系第一波
  对账门（spec 8000–8004 零缺位）+ 对账轮尝试 push。

## Out of Scope

- Wave 2（图进阶族）静脉不在本轮——下轮起逐件落。

## Testing Decisions

- 快照门 diff 仅 +4 逐行核对；对账门/覆盖门全绿；全仓
  verify BUILD SUCCESS。

## Further Notes

- 里程碑：V6/50（12%）。
