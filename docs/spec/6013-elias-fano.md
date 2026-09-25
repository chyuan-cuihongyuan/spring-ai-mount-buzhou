# Spec 6013 — Elias-Fano 单调序列编码（effort #6013，T13）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6225–T6226，impl 2213）。
> 借鉴：Elias-Fano 编码思想（Lucene/倒排索引同源）。

## Problem Statement

单调序列存储的病：long 数组直存 8 字节/元素（内存放大），
差分链表随机访问 O(n)（读放大）——**近下界压缩+O(1) 访问
面**缺失。

## Solution

`EliasFano`（core/message）：

- lowerWidth = ⌈log₂⌈U/n⌉⌉ 拆高低位：低位定宽直存、高位
  「值+下标」联合位图（每值一置位——间隔即增量）；
- get(i) O(1)（位图 select + 低位切片）；decode 全序还原；
- 读数：size/lowerWidth/storedBits（压缩率可见）；
- fail-fast：null/空/非单调、下标越界。

## User Stories

1. 作为索引作者，倒排表内存减半以上——同量级访问代价。
2. 作为审计作者，storedBits 显形——压缩收益可对账。

## Testing Decisions

- 八组密度用例（密集/稀疏/巨型宇宙/单元素/全同/全零/
  MAX_VALUE）往返全等；低位宽密度公式逐例钉住；密集序列
  storedBits < 32×n；防御性副本；fail-fast。

## Out of Scope

- 不做 rank/select 索引加速（测试规模逐位扫够用）；不做
  非单调输入（调用方先排序）。

## Further Notes

- 与 DeltaFrameOfReference（同包）同族不同面：分块差分参照
  vs 高低位联合位图；与 EliasGammaCodec 不同面：逐值前缀码
  vs 序列级拆分。
- 里程碑：T13/50（26%）。
