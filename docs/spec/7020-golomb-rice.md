# Spec 7020 — GolombRiceCodec 编码（effort #7020，U21）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7241–U7242，impl 2272）。
> 借鉴：Golomb 1966 / Rice 1971 变长码（FLIF/WebP lossless 同源）。

## Problem Statement

偏斜值域的病：定宽编码对几何分布值放大、Huffman 码表
短流不划算——**商 unary+余数定宽 k 位面**缺失。

## Solution

`GolombRiceCodec`（core/message，静态工具面）：商 unary
（1×q+0 终止）+余数 k 位 MSB-first；BitStream（位流+有效
位长）；k∈[0,30]、负值/越流 fail-fast；同值同码确定。

## Testing Decisions

- 手算向量（13,k=2→111001）；k=0..6 随机 roundtrip；
  偏斜小值压缩读数；越流 fail-fast。

## Out of Scope

- 不做自适应 k；不做有符号 zigzag。

## Further Notes

- 与 Simple8b（6015）同族不同面：几何自适应变长 vs 同域
  混合档位；与 EliasGammaCodec 不同面：参数化余数直存 vs
  全 unary。
- 里程碑：U21/50（42%）。
