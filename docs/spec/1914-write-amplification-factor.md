# Spec 1914 — LSM 写放大读面（effort #1914，R115）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T3029–T3030，impl 1515）。借鉴：
> RocksDB/LSM-Tree（万星级）write amplification 语义——落盘字节 /
> 逻辑写入字节 = 写放大因子：LSM 层级重写让同一份数据被反复写，
> WAF 是「盘在为谁工作」的账。落轮前 grep 复核：半衰期原选题被
> FactDecayPolicy（memory 置信衰减）占坑，换静脉。

## Problem Statement

LSM/分层存储的磁盘IO涨了三倍但业务写入没变：层间压实把同一数据
反复重写——写放大因子没有独立读面时，容量排查分不清「写多」还是
「重写多」。

## Solution

`WriteAmplificationFactor`（core/cleanup，静态纯函数）：

- `waf(bytesWrittenToDisk, logicalBytesIngested)`：落盘/逻辑写入
  比值（LSM 正常 2–10，越界即压实风暴信号）；
- `compactionDebtRatio(pendingCompactionBytes, diskCapacityBytes)`：
  待压实字节/盘容量——压实债读数（越高越逼近强制停写）；
- `needsThrottle(waf, threshold)`：WAF ≥ 阈值 → 建议写入限速。

## User Stories

1. 作为存储运维者，落盘 300GB/逻辑 100GB → WAF 3.0——压实重写
   三倍的账直读。
2. 作为容量作者，压实债 50GB/盘 500GB = 10%——离强制停写还有多远。
3. 作为限速作者，WAF ≥ 10 → 建议写入限速——风暴信号前置。

## Implementation Decisions

- 纯函数零状态；written ≥ 0、logical ≥ 1、debt ≥ 0、capacity ≥ 1、
  阈值 ≥ 1 fail-fast。

## Testing Decisions

- WAF 两例（3.0 精确/零写入 0.0）；压实债一例（0.1）；限速判定
  两例；畸形四型（负写入/零逻辑/负债务/负阈值）fail-fast。

## Out of Scope

- 不做真实压实执行（归存储层）；不做层级策略调参。

## Further Notes

- 与 墓碑占比读面（#1850）互补：那是删除堆积信号，这是写入重写
  信号；与 MergePressureReadout（R101 段数刻度）互补：那是段数
  维度，这是字节维度。
