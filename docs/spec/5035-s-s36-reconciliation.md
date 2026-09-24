# Spec 5035 — S 系 S36 周期对账（effort #5035，S36）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6171–T6172，impl 2186）。
> 对账门五十零号（S 系第六波）：Wave 6 五新类型快照补登 + S31
> 换静脉勘误收档 + 全仓 verify + 台账核账。

## Problem Statement

Wave 6（S31–S35）五件新类型未入公共面快照——全仓 verify
快照门必红；档案计数需对齐；S31 撞坑勘误（JumpHash 与
spec 3020 同面）需在对账轮正式收档。

## Solution

- 快照补登：1193→1198（WoundWaitGate——transaction /
  LeveledCompaction——recovery / BPlusTree——metrics /
  ContentDefinedChunking——fs / RadixTree——policy）；
- api-surface.md 同步 +5 行 + 勘误补 SegmentLog 行
 （S30 漏登——人工文档面不进门，但诚实补齐）；
- CONTEXT 计数 1193→1198；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- SSession5000LedgerAuditTest 台账核账（spec 5000–5034
  卅四轮四件套零缺位）；旧 jumphash 票文件清除（换静脉
  后 T6161/T6162 单义）。

## User Stories

1. 作为对账审计者，公共面快照与实际类型集一致——门不白设。
2. 作为后续轮作者，绿基线起跑。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳入 5000–5034）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：S36/50（72%）。
