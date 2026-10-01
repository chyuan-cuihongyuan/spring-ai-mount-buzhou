# Spec 11007 — Cepstrum 倒频谱（effort #11007，Y8）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11015–Y11016，impl 2460）。
> 借鉴：Bogert–Healy–Tukey 1963 思想——语音同源

## Problem Statement

周期性检测的对数域回声面——频谱周期结构的 quefrency 读出面。

## Solution

Cepstrum（core/metrics）：realCepstrum(double[])——FftIterative 正变换+逐仓 log(√(re²+im²)+ε)+共轭逆变换取实部；ε=1e-12 防零幅对数爆炸。

## Testing Decisions

1024 点 16 周期正弦倒谱峰位≈64 样点（±1 容差）+单位脉冲倒谱=平谱 log 恒值面+ε 防零（含零幅仓不炸）+确定性+fail-fast 五面。

## Out of Scope

不做复倒谱（相位保留另立）；不做功率倒谱面（|·|² 口径变体）；不做提升器（lifter 另立）。

## Further Notes

与 PeakDetector/LombScargle 同族异面：quefrency 域周期读数；消费 FftIterative 流程内自组合第三例；Wave 2 谱与语音族首件。
