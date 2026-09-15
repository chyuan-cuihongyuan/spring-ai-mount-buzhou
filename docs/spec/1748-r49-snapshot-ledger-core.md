# Spec 1748 — 快照再生与四面终核（effort #1748，R49）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2697–T2698，impl 1348）。
> 借鉴：N 会话 1600 系 R46/R47 与 L 会话 1400 系 R40 收口审计先例。

## Problem Statement

48 轮生产轮累计 48 个新公共类型 + README/api-surface/台账/地图四面账目
未终核：快照未再生、R39-R48 纵深行未登、并行会话在 main 留死链
（1133-budgetclamp-stats.md）——收口前必须四面一致。

## Solution

1. **快照再生**：`ApiSurfaceSnapshotTest#regenerateSnapshot`
   （-Dbuzhou.api-snapshot.regenerate=true，reactor 联编）——+48 公共类型
   入 `docs/api-surface.snapshot.txt`；
2. **README 纵深行吸收登记**：R39-R48 十行补登（覆盖门红→绿）；
3. **main 侧死链修复（吸收补登纪律）**：J 会话 R113 的
   `1133-budgetclamp-stats.md` spec 文件缺失但 README 行已在 main——
   依 main 实际代码（DefaultBudgetCalculator.BudgetClampStats 五字段）
   代写补登件，头注标明代登事实源；
4. **三门验证**：ApiSurfaceSnapshotTest + LSession1700LedgerAuditTest +
   SpecCoverageTest 全绿（reactor 联编下）。

## User Stories

1. 作为合并审查者，三门全绿证明四面一致可合。
2. 作为后续会话，main 无死链——覆盖门不再误伤。

## Implementation Decisions

- 吸收补登只写 main 上可验证的事实（代码+commit），不虚构 J 会话决策细节。
- 快照再生在合并 main 之后执行（吸收 1133 死链修复与并行漂移一并入账）。

## Testing Decisions

- 三门测试命令入档（reactor `-am` 联编下运行——单模块跑快照门必假红，
  诚实边界按测试头注入档）。

## Out of Scope

- 不改任何生产代码（除快照/文档工件）；不追 100% 覆盖。

## Further Notes

- 四面 = spec 文件 × README 纵深行 × api-surface（snapshot+md）×
  wayfinder 台账（tickets/impl/progress/map）。
