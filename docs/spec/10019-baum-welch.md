# Spec 10019 — BaumWelch（effort #10019，X20）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10039–X10040，impl 2422）。
> 借鉴：Baum–Welch 重估（Baum 1970——语音/生信 HMM 训练同源）

## Problem Statement

HMM 参数未知需从观测学习——E 步前向后向期望计数、M 步极大似然闭式重估，似然单调不减（EM 保证）。

## Solution

BaumWelch（core/eval，静态纯函数面）：reestimate(a,b,pi,obs,iterations)——γ/ξ 期望计数闭式重估 A/B/π+行随机归一+似然可观测面；迭代上限/维数 fail-fast。

## Testing Decisions

EM 似然单调不减圣像（10 随机模型×5 轮）+单步重估与全路径穷举后验互证+行随机归一核验+fail-fast 两面。

## Out of Scope

不做缩放变体（Viterbi 训练另立）；不做连续密度 HMM；不做收敛阈值自适应轮数。

## Further Notes

ForwardBackward（10018）的消费面；与 KMeans（已占）异域同 EM 家族。
