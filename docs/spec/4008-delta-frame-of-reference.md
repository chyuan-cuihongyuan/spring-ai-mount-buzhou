# Spec 4008 — 增量+基准帧编码（effort #4008，R9）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6017–T6018，impl 2109）。
> 借鉴：Parquet DELTA_BINARY_PACKED / ORC 列存思想。

## Problem Statement

时间戳/序号/偏移等单调（近单调）流「绝对值大、差值小」——直接
varint 每值 5–6 字节，增量后 1–2 字节——列存排序块压缩件缺失。

## Solution

`DeltaFrameOfReference`（core/message，帧化增量）：

- 每帧记基准值（frame of reference）原样，帧内余值只记与前一值
  的增量（delta）；增量与基准全走 zigzag varint（复用 VarintCodec）；
- 帧界即基准重置点——大幅跳变不被长程增量拖累；帧尾不足整帧以
  计数收官；Encoded(bytes, count) 记录面；
- decode count 驱动逐帧推进；截断/残留 fail-fast；frameSize=1
  全基准退化档。

## User Stories

1. 作为导出作者，单调列的字节面缩到 1/3 以下（千时间戳例证）。
2. 作为存储作者，分块边界跳变只付一次基准代价。

## Testing Decisions

- 千单调时间戳 roundtrip + <原 varint 1/3；两帧跳变重置 roundtrip；
  负值/混沌/极值半幅全域 roundtrip + 区段读；frameSize=1 退化档 +
  空编码；畸形六型 fail-fast（0 帧/null/截断/残留）。

## Out of Scope

- 不做 bit-packing 紧位打包（增量已走 varint）；不做字典编码
 （后续候选静脉）；不做流式游标面。

## Further Notes

- 与 VarintCodec 成对复用；与 EliasGamma/Huffman 成编码四档。
- 里程碑：9/50。
