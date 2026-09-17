# Spec 3039 — varint 编解码（effort #3039，R40）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5079–T5080，impl 2039）。
> 借鉴：Protobuf LEB128 + zigzag。

## Problem Statement

字节流里的整数（键值/计数/时间戳）定长 8 字节浪费——小值占绝大
多数；有符号数直接 LEB128 会负数顶格 10 字节（−1 与 MAX 同宽）。

## Solution

`VarintCodec`（core/message，纯函数静态件）：

- `zigzagEncode/Decode`：有符号 → 无符号交错映射（0→0/−1→1/
  1→2——负小值 1 字节起步）；
- `encode`：LEB128 7 位/字节小端，续位标记；小值 1 字节、值域
  每 7 位加一字节（300→2 字节、2⁴⁰→6 字节、极值 10 字节）；
- `decodeAt(bytes, offset)` 游标流式推进（截断/超 long 位宽
  fail-fast）；`decode` 整串恰一 varint（残留字节 fail-fast）。

## User Stories

1. 作为序列化作者，小值占多数的字节流紧凑——键值对/计数器/
   时间戳传输地基。
2. 作为流解析作者，游标连解免定界（续位自界定）。

## Testing Decisions

- zigzag 手算映射（0/−1/1/−2 双向）；字节长度阶梯（0/−1→1 字节、
  300→2、2⁴⁰→7、MIN/MAX→10）；−1000..1000 与七极值全往返；五值
  流游标连解至串尾；截断/残留/游标越界/超宽四路 fail-fast。

## Out of Scope

- 不做 varint 32 位截断口径（归调用方值域自律）；不做字节序
  变体（大端 LEB128 留白）；不做 ZigZag 浮点；不接具体序列化
  框架（接线归调用方）。

## Further Notes

- 与 EliasGammaCodec 成编码双件：varint 字节界（字节流）、
  gamma 位级前缀（位流）——按介质粒度选型。
- 里程碑：40/150。
