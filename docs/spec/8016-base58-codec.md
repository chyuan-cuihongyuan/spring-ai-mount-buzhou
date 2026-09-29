# Spec 8016 — Base58Codec Base58 编码（effort #8016，V17）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8033–V8034，impl 2318）。
> 借鉴：Bitcoin/IPFS/Flickr 思想（人类可转录字母表——0OIl 剔除）。

## Problem Statement

二进制标识人工转录的病：Base64 含 `+/&`（URL/双击选中
不友好）与易混字形（0/O、l/I）——**Base58 用 58 字符
无歧义字母表 + 大整数进制转换**换取可手抄可双击。

## Solution

`Base58Codec`（core/message，静态工具面）：58 字符表
（Bitcoin 字母表序——明示非 Flickr 序）大整数 base 转换；
`encode` 前导 0x00 字节 → '1' 字符（Bitcoin 经典约定）+
`decode` '1' 计数还原前导零 + 非法字符 fail-fast（携带
位置）+ null fail-fast + roundtrip 全等；确定性纯函数。

## Testing Decisions

- Bitcoin 官方向量（空→空；0x00→"1"；0x61→"2g"；hello
  → Cn8eVZg）；前导零多字节（00 00 01→"11"）；roundtrip
  300 随机（含前导零分布）；非法字符位置 fail-fast。

## Out of Scope

- 不做 Base58Check 校验和（双哈希面另件）；不做 Flickr序。

## Further Notes

- 与 Base32（若既有）同族不同面：58 无歧义转录表 vs 32
  大小写不敏感表。
- 里程碑：V17/50（34%）。
