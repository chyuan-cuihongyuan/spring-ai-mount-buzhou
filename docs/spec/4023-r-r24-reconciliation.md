# Spec 4023 — R 系 R24 周期对账（effort #4023，R24）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6047–T6048，impl 2124）。
> 对账门二十四号：Wave 4 五新类型快照补登 + 全仓离线 verify + 台账核账。

## Problem Statement

Wave 4（R19–R23）五件新类型未入公共面快照——全仓 verify 快照门
必红；档案计数需对齐。

## Solution

- 快照补登：1143→1148（CoDelController/QuicAmplificationWindow/
  CoopBudget/WorkStealingSplit/TailSamplingPolicy——backpressure×2+
  concurrent×2+observability×1 三段）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1143→1148；
- 全仓 16 模块离线 `mvn verify`（三门全绿；排除既有 flaky
  ShadowMirrorEndToEndTest 异步镜像竞态——单测绿全跑偶红，
  R18 已入档）；
- RSession4000LedgerAuditTest 台账核账（spec 4000–4022 廿三轮
  四件套零缺位）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳入 4000–4022）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：24/50=48%。
