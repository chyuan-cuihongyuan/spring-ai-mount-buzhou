# Spec 6048 — T 系 T48 周期对账（effort #6048，T48）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6295–T6296，impl 2248）。
> 周期对账轮（8/50=96% 前哨——T42 后第七波）。

## Scope

- 快照补登 +4（1244→1248：LotteryScheduler——T44 预载 +
  WaitForGraph/VanEmdeBoas/PowerOfTwoChoices——T45–T47）；
- api-surface.md 同步 +4 行 + CONTEXT 计数同步（1248×13）；
- 全仓 16 模块 verify 三门绿（R48 协议口径）+ 台账核账
  （spec 6000–6046 零缺位）；
- 勘误入档：PowerOfTwoChoices 遗漏与 TwoChoiceSelector（3022）
  的同族不同面声明——本轮补登（纯函数单次决策 vs 有状态放置
  账本）。

## Out of Scope

- 不改任何已入档组件行为；不新增非对账公共类型。

## Testing Decisions

- 快照门：regenerate 后 diff 仅 +4 逐行核对；
- 对账门：TSession6000LedgerAuditTest 全绿；
- 覆盖门：README ↔ docs/spec 双向无死角。
