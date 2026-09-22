# Spec 1905 — 分块压缩策略（effort #1905，R106）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3011–T3012，impl 1506）。借鉴：
> TimescaleDB（15K+ 星）chunk 压缩策略——分块超过 compress_after
> 年龄即压缩（历史不可变数据压缩率高、近期数据保持原样可写）；
> 读路径透明解压，节省比按压缩比直算。

## Problem Statement

历史数据治理只有删除一条路（保留策略）：老数据还想查——压缩换
空间但近期数据压缩会伤写路径；「哪些块该压、能省多少」缺独立
策略判定面。

## Solution

`ChunkCompressionPolicy`（core/cleanup，静态纯函数）：

- `shouldCompress(chunkAgeMillis, compressAfterMillis)`：年龄 ≥ 阈值
  即压缩（不可变后压缩，边界含上）；
- `savingsEstimate(originalBytes, compressionRatio)`：节省 =
  original×(1 − 1/ratio)——压缩比 3:1 省 2/3；
- `readPenaltyFactor(compressionRatio)`：读放大代价 = ratio（解压
  读的诚实代价面）。

## User Stories

1. 作为数据治理者，块龄 8 天/阈值 7 天 → 压缩——冷数据换空间。
2. 作为容量规划者，100GB×3:1 → 省 66.7GB——容量收益直算。
3. 作为查询作者，readPenaltyFactor=3 → 压缩块查询有代价预期——
   冷热分层知情。

## Implementation Decisions

- 纯函数零状态；age/compressAfter ≥ 0、ratio > 1（ratio ≤ 1 压缩
  无意义）fail-fast。

## Testing Decisions

- 压缩判定两例（恰阈值含上/未到阈值）；节省两例（3:1 省 2/3、
  1:1 零节省——但 ratio=1 被 fail-fast 拦，改 1.0001）；读代价
  直读；畸形三型 fail-fast。

## Out of Scope

- 不做真实压缩与解压（归存储层）；不做压缩算法选型。

## Further Notes

- 与保留策略（#65）互补：那是删除（有损），这是压缩（透明）；
  与墓碑占比互补：那是删除堆积信号，这是空间回收手段。
