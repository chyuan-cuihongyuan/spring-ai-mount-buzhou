# Spec 4029 — R 系 R30 周期对账（effort #4029，R30）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6059–T6060，impl 2130）。
> 对账门三十号：Wave 5 五新类型快照补登 + 全仓离线 verify + 台账核账。

## Problem Statement

Wave 5（R25–R29）五件新类型未入公共面快照——全仓 verify 快照门
必红；档案计数需对齐。

## Solution

- 快照补登：1148→1153（TopologySpreadPlacer/
  SupervisorRestartIntensity/SpeculativeStragglerPolicy/StableMatching/
  DynamicSnitchPenalty——policy×4+runaway×1+exec×1 三段）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1148→1153；
- 全仓 16 模块离线 `mvn verify`（三门全绿；排除 R18 已入档
  ShadowMirror flaky）；
- RSession4000LedgerAuditTest 台账核账（spec 4000–4028 廿九轮
  四件套零缺位）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳入 4000–4028）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：30/50=60%。
