# Spec 4006 — Huffman 前缀码（effort #4006，R7）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6013–T6014，impl 2107）。
> 借鉴：Huffman 1952；zlib/deflate 规范码（canonical）思想。

## Problem Statement

定长 8 位/字节对倾斜符号流（工具签名/文本骨架）的等概率浪费——
最优前缀码件缺失。

## Solution

`HuffmanCodec`（core/message，256 桶频率 → 规范码本）：

- 堆合并建树取码长（freq 并列按符号序确定性；单符号退化 1 位；
  树深 >57 fail-fast）；
- **规范 Huffman**：码字按 (码长, 符号) 字典序推导——码本只存
  码长（deflate 同款），firstCode 逐长左移推进；
- encode 位打包 MSB 先；decode 逐位对表（码长内首码偏移命中），
  截断/坏码 fail-fast；Encoded(bytes, bitCount) 记录面；
  codeLength/distinctSymbols 读数。

## User Stories

1. 作为导出作者，倾斜流的骨架压缩有最优前缀解——码表随码长走。
2. 作为对账审计者，频率分层码长（1/2/3/3）与均匀退化定长可测。

## Testing Decisions

- {100,50,20,10} 码长 1/2/3/3；均匀四符号全 2 位；千字节三层
  倾斜流 roundtrip + bitCount<2000（均匀要 8000）；单符号退化
  1 位 roundtrip；畸形八型 fail-fast（null/错长/全零/负频/未入表/
  位长超面/null data）。

## Out of Scope

- 不做自适应/两遍压缩流；不做码表序列化（归调用方）；
  不做 FSE/ANS（后续候选静脉）。

## Further Notes

- 与 EliasGamma（幂律整数无表）/Varint（字节界自界定）成编码
  三档按符号分布选型。
- 里程碑：7/50。
