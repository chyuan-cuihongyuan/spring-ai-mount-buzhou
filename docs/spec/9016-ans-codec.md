# Spec 9016 — rANS Codec 非对称数系熵编码（effort #9016，W17）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9033–W9034，impl 2369）。
> 借鉴：rANS（Duda 2009/Jarząbczyk——Zstd/LZFSE 嫡系同源）

## Problem Statement

静态熵编码的两难：Huffman 整符号离熵界有距、
算术编码逐位区间缩放慢——**rANS**：大整数态+商槽
频次编码+字节重整，两得。

## Solution

AnsCodec（core/message，静态工具面）：encode 自包含
流（长度+512B 表+8B 终态+重整字节）；decode 精确逆；
态域 [Q·M,2³⁶)；归一化恰 M。

## Testing Decisions

压缩率锚（单符号/偏斜文本）；4 文本+60 随机三
形态往返圣像；确定性；表损坏/短流 fail-fast。

## Out of Scope

不做自适应/交错多流（Interleaved rANS 面）；
不做 SIMD 解码；不做符号建模层（LZ 系消费方组合）。

## Further Notes

开发勘误入档：重整配对推导（编码阈 f·256Q/
解码阈 Q·M 每符号字节恰等）+归一化负频率死循环
根因（n=34 全异字节）。Wave 3 收束件。
