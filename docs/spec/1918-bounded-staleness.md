# Spec 1918 — 有界旧读（effort #1918，R119）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3037–T3038，impl 1519）。借鉴：
> Azure Cosmos DB（万星级）bounded staleness 一致性档——读允许落后
> 写至多 K 个单位（时间/版本）：「旧但有界」是强一致与最终一致
> 之间的可售合同。

## Problem Statement

读副本/缓存的旧度两极化：强一致贵、最终一致不敢用（旧多少没承诺）
——「旧不超过 X 秒」的合同缺判定面：某次读的旧度、当前配置下
是否达标，没有统一口径。

## Solution

`BoundedStaleness`（core/transaction，静态纯函数 + Staleness 枚举）：

- `stalenessMillis(lastWriteMillis, readMillis)`：读旧度 = read −
  lastWrite（与 0 取大；read < lastWrite 即时钟偏斜 → 负值钳 0
  诚实显示）；
- `verdict(staleness, boundMillis)`：旧度 ≤ bound → WITHIN_BOUND
  （达标）；否则 STALE（超界）。

## User Stories

1. 作为读路径作者，上次写 800ms 前、界 1000ms → WITHIN_BOUND——
   读副本合法。
2. 作为一致性评审者，界收紧到 500ms → 同一读变 STALE——合同
   收紧立即可见。
3. 作为排障者，旧度读数本身即信号——副本滞后再小也逃不出读数。

## Implementation Decisions

- 纯函数零状态；时点 ≥ 0、bound ≥ 0 fail-fast；read < lastWrite
  （时钟偏斜）旧度钳 0 而非负值——负旧度无意义。

## Testing Decisions

- 达标/超界两例；边界恰等含上达标；偏斜钳 0 一例；畸形两型
  fail-fast。

## Out of Scope

- 不做写时间戳追踪（归存储层）；不做一致性协议执行。

## Further Notes

- 与 QuorumConsistency（R77 配置判定）互补：那是副本配置账，这是
  单次读的旧度合同；与租约面互补：那是租约守恒，这是旧度守恒。
