# Spec 8029 — V 系 V30 周期对账（effort #8029，V30）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8059–V8060，impl 2331）。
> 对账轮（Wave 5 收口——30/50=60%）。

## Scope

- 快照批补登 +5（1307→1312：ThompsonSampler/AliasMethod/
  PoissonSampler/RejectionSampler——experiment + CusumDetector
  ——metrics）；
- api-surface.md 同步 +5 行 + CONTEXT 计数同步（1312×13）；
- 全仓 verify 三门绿（R48 协议口径——含 V24 环境勘误的
  fork 收尾挂起监控）+ V 系第五波对账门（spec 8000–8028
  零缺位）+ 对账轮尝试 push。

## Out of Scope

- Wave 6（评估聚类族）不在本轮。

## Testing Decisions

- 快照门 diff 仅 +5 逐行核对；对账门/覆盖门全绿；全仓
  verify BUILD SUCCESS。

## Further Notes

- 里程碑：V30/50（60%）。
