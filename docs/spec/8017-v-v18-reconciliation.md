# Spec 8017 — V 系 V18 周期对账（effort #8017，V18）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8035–V8036，impl 2319）。
> 对账轮（Wave 3 收口——18/50=36%）。

## Scope

- 快照批补登 +3（1299→1302：QuotientFilter——metrics +
  SipHash24——crypto + Base58Codec——message）；
- api-surface.md 同步 +3 行 + CONTEXT 计数同步（1302×13）；
- 全仓 16 模块 verify 三门绿（R48 协议口径）+ V 系第三波
  对账门（spec 8000–8016 零缺位）+ 对账轮尝试 push。

## Out of Scope

- Wave 4（堆结构族）不在本轮。

## Testing Decisions

- 快照门 diff 仅 +3 逐行核对；对账门/覆盖门全绿；全仓
  verify BUILD SUCCESS。

## Further Notes

- 里程碑：V18/50（36%）。
