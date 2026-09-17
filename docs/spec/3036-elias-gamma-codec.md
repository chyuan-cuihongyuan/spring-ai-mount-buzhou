# Spec 3036 — Elias gamma 编解码（effort #3036，R37）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5073–T5074，impl 2037）。
> 借鉴：Elias 1975 通用整数前缀码（gamma 码）。

## Problem Statement

幂律分布量（事件计数/频次/差分增量）定长 32 位传输浪费——小值
占绝大多数；通用变长编码缺一个零依赖、可流式自界定的地基件。

## Solution

`EliasGammaCodec`（core/message，纯函数静态件）：

- `encode(n)`：N=2^k+r → **k 零 + 边界 1 + k 位余数**（1→"1"、
  2→"010"、8→"0001000"——小数极短）；
- `decode(bits)` 整串恰一码字（残留位 fail-fast）；
- `decodeAt(bits, from)` 游标流式解码（零计数自定界——无解码表
  自同步推进，Decoded(value, nextOffset)）；
- `bitLength(n)` = 2k+1；n ≥ 1 校验。

## User Stories

1. 作为传输作者，幂律计数流紧凑编码——小值 1 位起步。
2. 作为流解析作者，游标推进连解——免定界符免解码表。

## Testing Decisions

- 手算码字五例；1..1000 全往返+码长=串长；四值流连解游标推进
  至串尾；幂律小数紧凑（1/31/63 → 1/9/11 位——定长 32 位对照）；
  六路 fail-fast（0/负 n、空串、残留位、零串无界、游标越界）。

## Out of Scope

- 不做 Elias delta/omega（变体族留白）；不做 Golomb-Rice（参数化
  前缀留白）；不做位级打包（String 位面——字节打包归调用方）；
  不做 0 与负数（zigzag 组合归 varint 件）。

## Further Notes

- 与 varint（LEB128）互补：varint 字节界自界定、gamma 位级前缀
  自界定——按介质粒度选型。
- 里程碑：37/150。
