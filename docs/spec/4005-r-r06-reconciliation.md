# Spec 4005 — R 系 R6 周期对账（effort #4005，R6）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6011–T6012，impl 2106）。
> 对账门六号：Wave 1 四新类型快照补登 + 全仓离线 verify + 台账核账。

## Problem Statement

Wave 1（R2–R5）四件新类型未入公共面快照——全仓 verify 快照门必红；
CONTEXT/api-surface 档案计数漂移需对齐。

## Solution

- 快照补登：1129→1133（CountMinSketch/SpaceSavingTopK/
  BoyerMooreMajority/KsTwoSample——metrics×3+eval×1 两段）；
- api-surface.md 同步 +4 行；CONTEXT 快照计数 984→1133（历史欠账
  一并对齐至真值）；
- 全仓 16 模块离线 `mvn verify`（快照门/覆盖门/对账门三门全绿）；
- RSession4000LedgerAuditTest 台账核账（spec 4000–4004 五轮四件套
  零缺位）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑（不欠快照债）。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门测试随 starter 模块在 verify 中
  自跑（范围自扩展——已纳入 4000–4004）。

## Out of Scope

- 不做 README/api-surface 之外的文档批量重整（历史欠账只对计数）。

## Further Notes

- 里程碑：6/50=12%。
