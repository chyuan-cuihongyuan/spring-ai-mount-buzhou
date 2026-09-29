# Spec 8026 — PoissonSampler（effort #8026，V27）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8053–V8054，impl 2328）。
> 借鉴：Knuth 1969 泊松采样思想。

## Problem Statement

离散事件计数的病：手工循环算阶乘溢出——**Knuth 乘法法：L=e^−λ 连乘 until U<L**，计数即样本。

## Solution

PoissonSampler（core/experiment）：of(λ,种子)+sample 单值+λ>0 越域 fail-fast+同种子同序列可回放+确定性。

## Testing Decisions

手锚（λ=1 众数 0/1）；大样均值/方差 vs λ 偏差 <5%（3000 样）+同种子同序列；fail-fast。

## Out of Scope

- 不做多维/自适应参数面（单参数语义明示）。

## Further Notes

- 里程碑：V27/50。
