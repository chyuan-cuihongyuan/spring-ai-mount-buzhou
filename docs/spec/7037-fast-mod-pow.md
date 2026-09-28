# Spec 7037 — FastModPow 快速模幂（effort #7037，U38）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7275–U7276，impl 2289）。
> 借鉴：平方-乘二进制快速幂思想（RSA/DH 原语面）。

## Problem Statement

模幂的病：朴素连乘 O(exponent)（指数放大不可承受）
——**指数二进制位扫描平方累积面**缺失。

## Solution

`FastModPow`（core/crypto，静态工具面）：O(log exponent)；
long 域安全上限（模数 ≤3.037e9 防中间乘积溢出——明示
fail-fast）；完全确定纯函数；费马小定理钉子+BigInteger
圣像；勘误：初版 5^10 mod 13 期望值口算错（12 非 11）
——圣像钉住修正。

## Testing Decisions

- 费马小定理（素域 a^(p−1)≡1）；200 随机 vs BigInteger
  圣像；零/一指数；安全域 fail-fast。

## Out of Scope

- 不做 Montgomery 乘法；不做大整数域（BigInteger 面）。

## Further Notes

- 与 ShamirSecretSharing（7024）同族不同面：门限分散 vs
  域算术原语。
- 里程碑：U38/50（76%）。
