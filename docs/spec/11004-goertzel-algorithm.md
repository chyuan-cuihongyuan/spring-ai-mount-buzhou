# Spec 11004 — GoertzelAlgorithm 单频检测（effort #11004，Y5）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11009–Y11010，impl 2457）。
> 借鉴：Goertzel 1958 思想——DTMF/电话信令同源

## Problem Statement

单频点能量检测——免全谱 FFT 的 O(n) 单音响应面。

## Solution

GoertzelAlgorithm（core/metrics）：power(double[],double,double)——ω=2πf/fs、系数 k=2cosω；递推 w[n]=x[n]+k·w[n−1]−w[n−2]；终值模平方 w[N]²+w[N−1]²−k·w[N]·w[N−1]=|X(f)|²（非归一 DFT 频仓模平方）。

## Testing Decisions

同频正弦高响应+异频正弦低响应比（>10 倍）+随机信号与直接 DFT 频仓模平方逐仓交叉互证 1e-9 圣像+直流/奈奎斯特边界拒绝+确定性+fail-fast 五面。

## Out of Scope

不做全谱扫描（FftIterative 已占）；不做双音 DTMF 判决树（消费方自组）；不做滑动窗流式面。

## Further Notes

免全谱的单频能量面——FFT 的 O(n) 单仓互补；Wave 1 变换族收束件。
