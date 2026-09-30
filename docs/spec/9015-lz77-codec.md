# Spec 9015 — LZ77 Codec 滑窗引用压缩（effort #9015，W16）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9031–W9032，impl 2368）。
> 借鉴：LZ77（Ziv-Lempel 1977——gzip/zlib 祖型；原 ZigZag 已含于 VarintCodec 换替补）

## Problem Statement

静态码表与建典的两头成本——**滑窗回引**：历史
即字典，(offset,length) token 替代重复段，码流
无需随行任何表。

## Solution

Lz77Codec（core/message，静态工具面）：encode(data,
windowSize)→List<Token>；decode 精确逆（重叠引用
逐字节回读）；MIN_MATCH=3；确定。

## Testing Decisions

AB 交替压缩锚；aaaa 自重叠锚；随机全谱纯字面
退化锚；4 文本×3 窗口+50 随机往返；确定性；
fail-fast 四面。

## Out of Scope

不做哈希链/树加速（朴素 O(n·w) 面——窗口尺寸
明示）；不做比特打包；不做动态窗口/懒惰匹配。

## Further Notes

与 LzwCodec（9012）同族不同面（LZ77 vs LZ78
两支）；W16 原选 ZigZag 已含于 VarintCodec 换此
替补（占坑复核勘误入档）。Wave 3 第四件。
