# Spec 6017 — T 系 T18 周期对账（effort #6018，T18）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6235–T6236，impl 2218）。
> 对账门六十三号（T 系第三波）：Wave 3 快照补登 + 全仓 verify
> + 台账核账。

## Problem Statement

Wave 3（T13–T17）五个新公共类型未入公共面快照；Wave 4
（T19–T23）五个源码已预载（本轮批入档——KdTree/QuadTree/
Geohash/HilbertCurve/InterpolationSearch，其 spec/票/impl/
README 随 T19–T23 落档）——快照门必红；第三波需对账封账。

## Solution

- 快照补登：1218→1228（Wave 3×5：EliasFano/GorillaXor/
  Simple8b/BitPacking/DictionaryEncoding——message +
  Wave 4 预载×5：KdTree/QuadTree/Geohash/HilbertCurve——
  policy + InterpolationSearch——concurrent）；
- api-surface.md 同步 +10 行；CONTEXT 计数 1218→1228；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- TSession6000LedgerAuditTest 台账核账（spec 6000–6016
  零缺位）；
- **spec 号回填勘误**：T13 轮起 spec 误跳 6012（6013–6018
  →6012–6017 平移回填，对账门公式复核钉住）。

## User Stories

1. 作为对账审计者，T 系第三波工件链四面互证全绿。
2. 作为后续轮作者，快照门恢复绿——继续推进。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳
  6000–6016）。

## Out of Scope

- 不做文档批量重整（只对计数与新行）。

## Further Notes

- 里程碑：T18/50（36%）。勘误入档：T13 整数除法算宽改
  ceilDiv+严格 ceil-log2；T16 63 位域 limit 溢出拆分校验；
  **环境确定性清零**：InMemoryStoresTest.leaseExpiresNaturally
  1ms TTL 在相邻语句间隙即可能过期（满载/JIT 假红——verify
  偶红单跑复绿定位），改轮询等自然到期（语义不变）。
