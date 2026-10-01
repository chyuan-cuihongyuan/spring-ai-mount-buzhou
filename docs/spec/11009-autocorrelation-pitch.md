# Spec 11009 — AutocorrelationPitch 自相关基音（effort #11009，Y10）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11019–Y11020，impl 2462）。
> 借鉴：Rabiner 1972 思想——语音基音检测同源

## Problem Statement

周期性基音提取——时域自相关滞后峰面（频域法的补位）。

## Solution

AutocorrelationPitch（core/metrics）：detectPitch(double[],double,double,double)——lag∈[fs/fMax, fs/fMin] 整数域扫归一化 ACF r(τ)=Σx[i]x[i+τ]/(Σx[i]²·重叠归一)；峰值 lag→fs/lag。

## Testing Decisions

fs=1000 正弦 100Hz 基音复原 ±2Hz+频域边界拒绝（fMin≥fMax）+时长不足 fail-fast+白噪归一峰<0.8 面+确定性+fail-fast 五面。

## Out of Scope

不做倍频程误判消歧（Cepstrum 消费方交叉）；不做 YIN/PM 变体（另立）；不做清浊判决面。

## Further Notes

时域基音原语——Cepstrum（11007）的时域镜像面；Wave 2 谱与语音族第三件。
