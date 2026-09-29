# Spec 8015 — SipHash 密钥化哈希（effort #8015，V16）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8031–V8032，impl 2317）。
> 借鉴：Aumasson & Bernstein 2012（Redis/Python/Rust HashMap 哈希 DoS 防御思想）。

## Problem Statement

无钥哈希的病：公开哈希函数可被对抗者**离线预造全碰撞键
集**（哈希 flood DoS——表退化成链）—— keyed 哈希让碰撞
预计算不可行（密钥每进程随机）。

## Solution

`SipHash24`（core/crypto，静态工具面）：SipHash-2-4 标准
——64 位密钥（k0/k1 两 long）+SipRound 四轮压缩两轮终化
（2 compression rounds、4 finalization rounds——名字即参数）；
`hash(long k0, long k1, byte[] data)` long 值；参考向量钉死
（RFC/作者官方向量：空串与 "a".. 字节序列——实现正确性
的官方锚）；null 数据 fail-fast；非加密承诺明示（MAC 级
不承诺——PRF 面向哈希表防护）。

## Testing Decisions

- 官方向量逐字节钉住（k=00..0f，空串→726fdb47dd0e0e31
  等标准序列）；密钥敏感性（改一位密钥雪崩）；同键同密钥
  确定性/异密钥异值；null fail-fast。

## Out of Scope

- 不做 SipHash-1-3 变体；不承诺 MAC 安全性（明示）。

## Further Notes

- 与 XxHash64（7030）同族不同面：非加密高速哈希 vs
  密钥化对抗碰撞预计算。
- 里程碑：V16/50（32%）。
