# Spec 9030 — Miller-Rabin 素性检测（effort #9030，W31）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9061–W9062，impl 2383）。
> 借鉴：Miller 1976/Rabin 1980（JDK BigInteger.isProbablePrime 同源——RSA 密钥生成底座）

## Problem Statement

Fermat 小定理检测被 Carmichael 数全骗——
**强伪素判定**：非平凡平方根存在即合数，
12 见证基 long 域确定性。

## Solution

MillerRabinPrimality（core/crypto，静态纯函数面）：
isPrime(n)——见证基固定集确定性；BigInteger
作 128 位乘模载体。

## Testing Decisions

小素/边界/素数平方锚；9 个 Carmichael+
2-PRP 341/2047 识破；400 随机 long 三域与
BigInteger(50) 互证全等。

## Out of Scope

不做素数生成/nextPrime；不做因子分解；
不做随机见证可插拔（确定性承诺明示）。

## Further Notes

与 FastModPow（同包）同域不同面。
Wave 6 第一件。
