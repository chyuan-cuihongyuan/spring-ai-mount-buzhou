# Spec 8033 — ViterbiDecoder（effort #8033，V34）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8067–V8068，impl 2335）。
> 借鉴：Viterbi 1967 维特比解码思想（CDMA/GSM 同源）。

## Problem Statement

HMM 最可能状态的病：全路径枚举 O(S^T)——**维特比：动态规划按时刻保最大路径概率+回溯**。

## Solution

ViterbiDecoder（core/eval）：of(初始/转移/发射概率表)+decode 最可能状态序列+表非随机归一/维度不符/空观测 fail-fast+确定性。

## Testing Decisions

天气-伞经典手锚；全观测暴力枚举小域圣像全等；fail-fast。

## Out of Scope

- 不做在线增量/分布式面（单机批语义明示）。

## Further Notes

- 里程碑：V34/50。
