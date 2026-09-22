# Spec 1900 — 合并压力读面（effort #1900，R101）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3001–T3002，impl 1501）。借鉴：
> ClickHouse（30K+ 星）MergeTree「too many parts」语义——每分区活跃
| > part 数逼近建议上限即 WARN、越过硬上限直接拒绝插入（安全阀）；
> 写放大与合并积压用同一把刻度读。

## Problem Statement

高频小写入攒出的碎片化 part/段：合并跟不上时活跃段堆积——到拒绝
那一刻才暴露（写入开始失败），此前压力无刻度；「还能不能写、该
不该降频」缺独立判定面。

## Solution

`MergePressureReadout`（core/recovery，静态纯函数 + Pressure 枚举）：

- `pressure(activeParts, recommendedMax)`：活跃段/建议上限占比读数；
- `verdict(ratio, warnAt)`：≥ 1.0 → REJECT；≥ warnAt → WARN；否则
  OK（边界含上）；
- `shouldRejectInsert(activeParts, hardMax)`：硬上限安全阀——超过
  即拒新写入（ClickHouse too many parts 同款）。

## User Stories

1. 作为存储运维者，活跃 150/建议 300 = 0.5 → WARN——降频写入
   或加快合并。
2. 作为兜底者，活跃 300/建议 300 → REJECT；硬阀 400 独立于建议线
   ——拒绝语义双层。
3. 作为观测者，pressure 读数接健康面——合并积压提前可见。

## Implementation Decisions

- 纯函数零状态；active ≥ 0、建议上限 ≥ 1、硬上限 ≥ 建议上限、
  warnAt ∈ (0,1] fail-fast；判定只读。

## Testing Decisions

- 三态各一例；边界含上两例（恰 warnAt/恰 1.0）；硬阀独立一例
  （硬上限内不拒）；畸形三型 fail-fast。

## Out of Scope

- 不做真实合并执行（归存储层）；不做分区级细分。

## Further Notes

- 与墓碑占比读面（#1850）互补：那是删除堆积，这是写入碎片堆积；
  与 MaintenanceTrigger 互补：那是触发器，这是压力刻度。
