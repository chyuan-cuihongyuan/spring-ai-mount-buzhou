# Spec 9031 — Karatsuba 大数乘法（effort #9031，W32）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9063–W9064，impl 2384）。
> 借鉴：Karatsuba 1960（分治乘法——GMP/OpenSSL 大数运算同源）

## Problem Statement

竖式逐位乘 O(n²)——**Karatsuba**：对半分裂
三次子乘替代四次，O(n^1.585)。

## Solution

KaratsubaMultiplication（core/crypto，静态
纯函数面）：multiply(BigInteger,BigInteger)；
1024 bit 阈值落原生；符号归一。

## Testing Decisions

1234×5678 手锚；符号四象限；2048/4096/
8192 bit 各 10 组 BigInteger 恒等；不对称位宽；
确定性；fail-fast。

## Out of Scope

不做 Toom-Cook/FFT 乘法（更高阶面）；
不做原语位运算载体（BigInteger 载体明示）；
不做除法/开方。

## Further Notes

与 FastModPow 同域不同面；与
ChineseRemainder（同包）互补。Wave 6 第二件。
