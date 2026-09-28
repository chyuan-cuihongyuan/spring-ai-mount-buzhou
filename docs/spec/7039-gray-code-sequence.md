# Spec 7039 — GrayCodeSequence 格雷码序列（effort #7039，U40）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7279–U7280，impl 2291）。
> 借鉴：Frank Gray 1953 二进制反射格雷码思想。

## Problem Statement

机械计数的病：多位同时翻转的瞬态歧义——**相邻恰一位
差的反射格雷码面**缺失。

## Solution

`GrayCodeSequence`（core/metrics，静态工具面）：encode=
i⊕(i>>1)；decode 逐位前缀异或逆变换；n 位全序列
（n∈[1,30]——2^n 爆炸明示拒绝，单项面不限）；相邻恰
一位差性质；确定性纯函数；负数 fail-fast。

## Testing Decisions

- n=2/3 手锚序列；n=10 相邻单 bit 翻转性质；0..4095
  encode/decode 互逆；边界 fail-fast。

## Out of Scope

- 不做 n≥31 全序列（爆炸面）；不做非反射变体。

## Further Notes

- 与 BitPacking（message）同族不同面：位打包存储 vs 位
  翻转序。
- 里程碑：U40/50（80%）。
