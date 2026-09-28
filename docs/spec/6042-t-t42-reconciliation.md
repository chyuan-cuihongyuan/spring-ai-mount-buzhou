# Spec 6042 — T 系 T42 周期对账（effort #6042，T42）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6283–T6284，impl 2242）。
> 对账门六十七号（T 系第七波）：Wave 7 快照补登 + 全仓 verify
> + 台账核账。

## Problem Statement

Wave 7（T37–T41）五个新公共类型未入公共面快照——全仓
verify 快照门必红；第七波需对账封账。

## Solution

- 快照补登：1238→1244（Wave 7×5：CountingBloomFilter/
  StableBloomFilter——metrics + WeightedReservoirSampler——
  policy + MedianFinder——concurrent + AimdWindow——
  ratelimit，另 McsLock——concurrent 为 T43 预载）；
- api-surface.md 同步 +7 行；CONTEXT 计数 1238→1244；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- TSession6000LedgerAuditTest 台账核账（spec 6000–6041
  零缺位）。

## User Stories

1. 作为对账审计者，T 系第七波工件链四面互证全绿。
2. 作为后续轮作者，快照门恢复绿——继续推进。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳
  6000–6041）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：T42/50（84%）。勘误入档：T37 原 spec 号 6035 与
  T36 对账轮撞号（票 T6261/62 与 Buddy 撞号）——平移回填
  spec 6036/票 T6273–74。
