# Spec 5041 — S 系 S42 周期对账（effort #5041，S42）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6183–T6184，impl 2192）。
> 对账门五十零号（S 系第七波）：Wave 7 五新类型快照补登 +
> 全仓 verify + 台账核账。

## Problem Statement

Wave 7（S37–S41）五件新类型未入公共面快照——全仓 verify
快照门必红；档案计数需对齐。

## Solution

- 快照补登：1198→1203（EpochReclamation——concurrent /
  DistributedSnapshot——observability / ShuffleSharding——
  policy / SlruCache——cache / BlockedSlidingCounter——
  metrics）；
- api-surface.md 同步 +5 行；CONTEXT 计数 1198→1203；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- SSession5000LedgerAuditTest 台账核账（spec 5000–5040
  卌轮四件套零缺位）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳入 5000–5040）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：S42/50（84%）。
