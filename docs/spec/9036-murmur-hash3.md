# Spec 9036 — MurmurHash3 高速哈希（effort #9036，W37）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9073–W9074，impl 2389）。
> 借鉴：MurmurHash3（Appleby 2008——Guava/Redis/Cassandra 同源；原 HOTP 已含于 TotpGenerator（8038 的 hotp 核心）换替补）

## Problem Statement

String.hashCode 单乘弱雪崩（相邻输入高位
不变）——**MurmurHash3**：三轮旋转混合+
终结化，非加密高速雪崩标杆。

## Solution

MurmurHash3（core/metrics，静态纯函数面）：
hash32(data,seed)——x86 32 位；尾块三变体；
同种子确定可回放。

## Testing Decisions

空串 seed=0→0 参考锚；雪崩圣像（单位
翻转均翻 14..18 位）；10 万输入 >99%
无碰撞；尾块确定性；fail-fast。

## Out of Scope

不做 128 位 x64 变体（另立面）；不做
密钥化（SipHash24 面）；不做增量流式。

## Further Notes

与 SipHash24（crypto）同域不同面；W37
原选 HOTP 已含于 TotpGenerator（8038）
换替补（占坑复核勘误）。Wave 7 第一件。
