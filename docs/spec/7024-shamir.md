# Spec 7024 — ShamirSecretSharing 门限共享（effort #7024，U25）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7249–U7250，impl 2276）。
> 借鉴：Shamir 1979 门限秘密共享思想。

## Problem Statement

密钥保管的病：单点保存（丢失/被窃即全损）——**多项式
门限分散面（信息论安全）**缺失。

## Solution

`ShamirSecretSharing`（core/crypto）：秘密为 GF(257) 上
t−1 次多项式常数项；n 份 (x,y) 分发（字节域无损嵌入
素域 257——模运算免乘法表）；任意 t 份拉格朗日插值
还原；种子化 Random 可回放；重复 x/份数越域/门限<2
fail-fast。

## Testing Decisions

- 0..255 步进 17 全还原；C(5,3) 全子集还原；少门限/
  重复 x/越域 fail-fast；种子确定性。

## Out of Scope

- 不做 GF(256) 本原多项式变体；不做份额校验和。

## Further Notes

- 与 EnvelopeCipher（同包）同族不同面：对称加密单点
  密钥 vs 门限分散无单点信任。
- 里程碑：U25/50（50%）。
