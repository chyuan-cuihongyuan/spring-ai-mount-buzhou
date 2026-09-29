# Spec 8042 — V 系 V42 周期对账（effort #8042，V42）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8085–V8086，impl 2344）。
> 对账轮（Wave 7 收口——42/50=84%）。

## Scope

- 快照批补登 +5（1317→1322：FeistelNetwork/ConstantTimeEquals/
  TotpGenerator/HammingCode/ChineseRemainder——crypto）；
- api-surface.md 同步 +5 行 + CONTEXT 计数同步（1322×13）；
- 全仓 verify 组合定向口径（R48 环境豁免——fork 间歇崩溃勘误
  延续）+ V 系第七波对账门（spec 8000–8041 零缺位）+ 对账轮
  尝试 push；
- 勘误入档：ReedSolomon 退雾区（ChineseRemainder 补位——V41）。

## Out of Scope

- Wave 8（对齐时序族）不在本轮。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿；组合定向
  verify 绿。

## Further Notes

- 里程碑：V42/50（84%）。
