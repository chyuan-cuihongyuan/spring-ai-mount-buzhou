# Spec 11008 — ZeroCrossingRate 过零率（effort #11008，Y9）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11017–Y11018，impl 2461）。
> 借鉴：语音/MIR 特征惯例（librosa zero_crossing_rate 同源）

## Problem Statement

信号符号翻转频率——最廉价的频率/噪声度特征面。

## Solution

ZeroCrossingRate（core/metrics）：rate(double[])——统计 sign(x[i])≠sign(x[i+1]) 次数 /(n−1)；零值不构成符号（沿用前有效符号口径）。

## Testing Decisions

交变 ±1 序列恒 1+常量恒 0+正弦手锚（fs=1000、f=50、N=1000：过零≈100 次/999）+零值沿用面（0 插入不增率）+确定性+fail-fast 五面。

## Out of Scope

不做分帧/滑窗统计（帧层面消费方自组）；不做过零点位置列表（只读率面）。

## Further Notes

MIR/语音最简频度特征；Wave 2 谱与语音族第二件。
