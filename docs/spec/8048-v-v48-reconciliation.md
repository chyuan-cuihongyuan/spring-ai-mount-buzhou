# Spec 8048 — V 系 V48 周期对账（effort #8048，V48）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8097–V8098，impl 2350）。
> 对账轮（Wave 8 收口——48/50=96%）。

## Scope

- 快照批补登 +5（1322→1327：NeedlemanWunsch/SmithWaterman/
  SaxCodec/PaaCodec——metrics + GotohAlignment——eval）；
- api-surface.md 同步 +5 行 + CONTEXT 计数同步（1327×13）；
- 全仓 verify 组合定向口径（R48 环境豁免——fork 间歇崩溃
  勘误延续）+ V 系第八波对账门（spec 8000–8047 零缺位）+
  对账轮尝试 push；
- 号段勘误入档：8039 因 ReedSolomon 换静脉弃号、后续轮顺延
  ——V50 收口 spec 8050 溢出原号段上界 1 位（诚实声明）。

## Out of Scope

- V49 天际线独件与 V50 收口不在本轮。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿；组合定向
  verify 绿。

## Further Notes

- 里程碑：V48/50（96%）。
