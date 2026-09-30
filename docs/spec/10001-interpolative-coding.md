# Spec 10001 — InterpolativeCoding 二分内插编码（effort #10001，X2）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10003–X10004，impl 2404）。
> 借鉴：Interpolative Coding（Moffat–Stuiver 2000——Lucene block postings 倒排压缩同源）

## Problem Statement

倒排表/时间戳列等单调整数流按逐元素
定宽 32 位存储——高位冗余；Elias–Fano
已占（core/message），需上下文递归姊妹面。

## Solution

InterpolativeCoding（core/concurrent，静态纯
函数面）：encode/decode——中点递归二分，
中点元素以上下文区间 [low, high] 定宽
（bits(high−low)）写入，两半递归；末元素
32 位播种解码上界；非负+非严格单调契约。

## Testing Decisions

手锚 [2,3,5,8,13]；200 随机单调列往返全等；
稠密连续/重复/单零/全域边界；压缩率圣像
（等距千列 < 原始一半）；确定性双跑；
fail-fast 六面（null/空/负值/乱序/解码 null）。

## Out of Scope

不做分桶位流（EliasFano 已占异面）；不做
有符号/乱序输入（契约 fail-fast）；不做
流式增量编码（批量面即可）。

## Further Notes

与 EliasFano（core/message）同域不同面：
上下文递归 vs 分桶位流；与 VarintCodec
不同面：有序统计压缩 vs 通用字节变长。
Wave 1 首件。
