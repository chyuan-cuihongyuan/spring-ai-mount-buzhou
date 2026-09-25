# Spec 6014 — Gorilla XOR 浮点压缩（effort #6014，T14）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6227–T6228，impl 2214）。
> 借鉴：Facebook Gorilla/Prometheus TSDB 思想。

## Problem Statement

时间序列浮点存储的病：逐值 8 字节直存（本地性序列信息冗余
极高，存储放大）——**相邻 XOR 有效位窗压缩面**缺失。

## Solution

`GorillaXor`（core/message）：

- 相邻 double 位模式 XOR 三态编码：同值 1 位 0；有效位
  落前值窗内复用（'10'+有效位）；否则新窗（'11'+5 位
  前导+6 位有效长−1+有效位，前导 5 位钳 31）；
- decompress 无损按位还原（rawBits——NaN 载荷/±0/Inf 保真）；
- 读数：compressedBits/originalBits/count（压缩率对账）；
- fail-fast：null 输入。

## User Stories

1. 作为 TSDB 作者，量化步进行情流压缩率 >3×——存储减负。
2. 作为审计作者，同输入同位流——编码确定性可回放。

## Testing Decisions

- 量化步进 500 点无损+压缩位 <1/3 原始；随机位模式 200 点
  无损；特殊值（±0/Inf/奇载荷 NaN）按位保留；全同序列
  恒 64+99 位钉住；位流确定性；fail-fast。

## Out of Scope

- 不做时间戳列压缩（Gorilla 全家桶只取浮点列）；不做流式
  分块；不做有损预测（delta-of-delta 面）。

## Further Notes

- 与 VarintCodec/DeltaFrameOfReference（同包）同族不同面：
  整数变长/差分参照 vs 浮点 XOR 有效位窗。
- 里程碑：T14/50（28%）。
