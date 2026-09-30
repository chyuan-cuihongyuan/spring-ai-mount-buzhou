# Spec 9012 — LZW Codec 字典压缩（effort #9012，W13）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9025–W9026，impl 2365）。
> 借鉴：LZW（Ziv-Lempel 1978/Welch 1984——GIF/UNIX compress/PDF 同源）

## Problem Statement

静态熵编码需随行码表（Huffman）/回窗引用
（LZ77）——**自适应字典**：256 单字节起步、扫描中
同步生长，码流自带词典。

## Solution

LzwCodec（core/message，静态工具面）：encode(byte[])
→ List<Integer> 码字（首码 256 起）；decode 精确逆
（KwKwK 特例）；null/非法码流 fail-fast。

## Testing Decisions

TOBEORNOTTOBE 首十码锚；重复串压缩率锚；
KwKwK 特例锚；60 随机二进制全谱往返圣像；
确定性；fail-fast。

## Out of Scope

不做比特打包（码字序列面——位流归 Varint/
Huffman 邻居）；不做字典容量上限/清码（GIF 变体
面）；不做流式。

## Further Notes

与 HuffmanCodec/VarintCodec（同包）不同面。
Wave 3 编码压缩族第一件。
