# Spec 9037 — Diffie-Hellman 密钥交换（effort #9037，W38）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9075–W9076，impl 2390）。
> 借鉴：Diffie-Hellman 1976（「New Directions in Cryptography」开山——TLS/SSH/Signal 密钥协商同源）

## Problem Statement

对称密钥需安全信道预分发——**DH**：素域
公开互传 g^a/g^b，双方同达 g^(ab)，离散
对数难解保密。

## Solution

DiffieHellmanExchange（core/crypto，静态
纯函数面）：publicKey/sharedSecret/
generatePrivateKey；素性校验 long 域复用
MillerRabin（9030）。

## Testing Decisions

教科书 p=23 例手锚逐值；20 轮随机双向
同达；公钥≠私钥；fail-fast 七面。

## Out of Scope

不做椭圆曲线 ECDH；不做认证（MITM
防护归签名层）；不内置生产域参数
（教学常量明示非安全）。

## Further Notes

与 ShamirSecretSharing 同域不同面；
开发勘误：记忆 RFC 常量 3476 位非素——
探针证伪换教学素数诚实命名。Wave 7 第二件。
