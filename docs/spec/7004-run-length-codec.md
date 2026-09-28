# Spec 7004 — RunLengthCodec 行程编码（effort #7004，U5）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7209–U7210，impl 2256）。
> 借鉴：RLE（PNG/传真 G3 同源思想）。

## Problem Statement

高重复数据的病：重复率高的列/位图逐值直存（存储随
行程数放大）——**连续重复段 (count,value) 折叠面**缺失。

## Solution

`RunLengthCodec`（core/message，静态工具面）：

- 连续重复段折叠 (count,value) 对，count∈[1,255]（超长
  行程切段）；全重复列压至 n/255 对，全相异列诚实膨胀
  2×（对账读数可感不隐瞒反面）；
- decode 奇长度/零计数 fail-fast（对偶不完整与零长行程
  非法）；encode/decode/pairCount；null fail-fast。

## User Stories

1. 作为存储作者，位图/状态列行程折叠省存储。
2. 作为审计作者，pairCount 显形压缩比。

## Testing Decisions

- 300 随机行程列 roundtrip 全等；600 长行程切 3 对逐字节
  钉住（255/255/90）；全相异 5 值 5 对诚实膨胀；全重复
  300 值 2 对；空列空对；fail-fast 五路。

## Out of Scope

- 不做变长 count 位打包（Simple8b 面）；不做游程树。

## Further Notes

- 与 DictionaryEncoding（6017）同族不同面：局部重复折叠
  vs 全列基数收缩；与 Simple8b（6015）不同面：行程对 vs
  同域变长位打包。
- 里程碑：U5/50（10%）。
