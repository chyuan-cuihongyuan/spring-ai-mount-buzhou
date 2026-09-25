# Spec 6016 — Bit Packing 固定位宽打包（effort #6016，T16）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6231–T6232，impl 2216）。
> 借鉴：Parquet/ORC bit-packing 思想。

## Problem Statement

窄值域整数列存储的病：全量 long 直存（值域窄时放大数十
倍）——**精确可控位宽串接面**缺失。

## Solution

`BitPacking`（core/message）：

- 统一位宽 w∈[0,64] 无缝串接（值 i 占 [i·w,(i+1)·w) 位，
  跨字双字移位合并 O(1) 取值，64 位字界零浪费）；
- 读数：bitWidth/count/wordCount（⌈n·w/64⌉）/packedCopy；
- fail-fast：null、w 越域、负值、值溢出位域。

## User Stories

1. 作为列存作者，8 位枚举列压缩 8×——存储精确可控。
2. 作为审计作者，打包流静态可解——离线可核。

## Testing Decisions

- 位宽 {0,1,3,7,31,32,33,63,64} 往返全等（随机值域随宽
  定参）；宽 7×100 值跨 11 字逐值取值钉住；确定性；
  fail-fast（含 63 位域校验溢出勘误）。

## Out of Scope

- 不做 SIMDBP-128 分块变体；不做与 RLE 组合（Parquet
  编码组合层）。

## Further Notes

- 与 Simple8b（T15）互补：单一固定宽 vs 混合自适应档位；
  与 VarintCodec 不同面：位域定宽 vs 字节流变长。
- 里程碑：T16/50（32%）。
