# Spec 7038 — ChiSquareUniformity 均匀性检验（effort #7038，U39）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7277–U7278，impl 2290）。
> 借鉴：Pearson 1900 卡方拟合优度思想。

## Problem Statement

随机质量审计的病：「看起来均匀」无量化面——**χ² 统计量
+逐桶贡献分解面**缺失。

## Solution

`ChiSquareUniformity`（core/metrics，静态工具面）：χ²=
Σ(观测−期望)²/期望，自由度 k−1；逐桶贡献读数（哪桶
偏了可定位）；期望 ≥1 诚实下限；不假装 p 值精确（阈值
调用方给——明示）；越桶域 fail-fast；手算勘误（初版
[3,1] 双桶期望 2 的 χ² 口算 2.0 实为 1.0——钉住修正）。

## Testing Decisions

- 均匀序列 χ²≈自由度 vs 偏斜序列巨大对比；双桶手算
  1.0；自由度读数；fail-fast。

## Out of Scope

- 不做 p 值分布表；不做独立性检验。

## Further Notes

- 与 CoefficientOfVariation 同族不同面：离散度单值 vs
  分桶拟合优度。
- 里程碑：U39/50（78%）。
