# Spec 5033 — Content-Defined Chunking 内容定义分块（effort #5033，S34）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6167–T6168，impl 2184）。
> 借鉴：restic/rclone FastCDC（Gear 滚动哈希 + 归一化双掩码思想）。

## Problem Statement

字节流切块的病：定长切块（插入/移位后全量块失效——
去重率崩塌）——**边界只由内容决定面**缺失。

## Solution

`ContentDefinedChunking`（core/fs）：

- `chunk(byte[])`：Gear 滚动哈希（64 位，旧字节自然移出
  无显式滑窗）低位掩码命中即切；minSize 内不切、maxSize
  硬上限强切、归一化点后换更难命中的 L 掩码（切点偏大、
  方差收窄——FastCDC 双掩码）；
- Gear 表由 SplitMix64 种子生成（无随机、跨进程同表——
  固定字节序列固定切分，确定性可回放）；
- 块 `Chunk(offset,length)` 覆盖连续不重叠、拼接还原全量；
- fail-fast：null 数据、min<1、max≤min、掩码位宽越界、
  块 offset/length 畸形。

## User Stories

1. 作为备份作者，插入只影响相邻块——其余块原样去重。
2. 作为同步作者，同内容同切分——跨端边界一致可对账。

## Testing Decisions

- 圣像边界钉住（Python 64 位语义预演：4096 字节 LCG 数据 →
  304 块，首八块 (0,11)/(11,10)/(21,9)/(30,8)/(38,11)/
  (49,13)/(62,8)/(70,10)，末块 (4086,10)）；覆盖连续拼接
  还原；非末块长度 ∈[min,max]；同内容同参数同切分；
  单字节翻转——翻转点之前边界全等、整体切分不同（CDC
  局部性核心性质）；短数据/恰好 min 单块；畸形 fail-fast。

## Out of Scope

- 不做哈希指纹与去重存储（本件是切块语义面）；不做
  滚动窗口变体（BuzHash/Rabin 指纹不同面）；不做压缩。

## Further Notes

- 与 SegmentLog（spec 5028）同族不同面：字节流切块去重面 vs
  追加日志保留面。Wave 6 第四件。
- 里程碑：S34/50（68%）。
