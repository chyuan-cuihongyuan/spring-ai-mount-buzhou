# Spec 6023 — T 系 T24 周期对账（effort #6023，T24）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6247–T6248，impl 2224）。
> 对账门六十四号（T 系第四波）：Wave 4 文档落档核账 + 全仓
> verify。快照已于 T18 批补齐（1228 含 Wave 4 预载×5）。

## Problem Statement

Wave 4（T19–T23）文档落档后需四面互证核账——工件链
（spec/票/impl/README）零缺位确认与全仓三门回归。

## Solution

- 快照无需变更（1228 已含 Wave 4×5，T18 批补登）；
- 全仓 16 模块 `mvn verify`（三门全绿；R48 协议口径）；
- TSession6000LedgerAuditTest 台账核账（spec 6000–6022
  零缺位严格递增）。

## User Stories

1. 作为对账审计者，T 系第四波工件链四面互证全绿。
2. 作为后续轮作者，预载模式（源码先行+文档落档）经核账
   确认可复用。

## Testing Decisions

- 全仓 verify 退出码 0 即验；对账门自跑（范围已纳
  6000–6022）。

## Out of Scope

- 不做文档批量重整。

## Further Notes

- 里程碑：T24/50（48%）。勘误入档：**环境确定性清零第二例**
  ——LeaseRenewalReadoutTest.consecutiveRenewalsKeepWatermark
  Monotonic 断言 min 水位跨毫秒抖动可比 first 小 1ms（600ms
  TTL 首续即耗 1ms），改 ≤first（合同本意「min 不被第二次
  更大剩余值抬高」语义不变）——三连跑绿确认。
