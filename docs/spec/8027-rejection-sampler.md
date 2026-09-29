# Spec 8027 — RejectionSampler（effort #8027，V28）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8055–V8056，impl 2329）。
> 借鉴：von Neumann 1951 拒绝采样思想。

## Problem Statement

非标准分布采样的病：无解析逆——**包络拒绝：候选 x~M，U·M(x)≤f(x) 收下否则重掷**——接受率=面积比可感。

## Solution

RejectionSampler（core/experiment）：of(目标密度/包络常数/种子，域内均匀候选)+sample+密度负值/包络不足探测（尝试上限）fail-fast+同种子可回放。

## Testing Decisions

手锚（三角密度均值）；大样直方 vs 密度形状（分桶占比偏差）+包络不足上限拒绝；fail-fast。

## Out of Scope

- 不做多维/自适应参数面（单参数语义明示）。

## Further Notes

- 里程碑：V28/50。
