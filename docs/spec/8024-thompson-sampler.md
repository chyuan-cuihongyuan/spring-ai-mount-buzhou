# Spec 8024 — ThompsonSampler（effort #8024，V25）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8049–V8050，impl 2326）。
> 借鉴：Thompson 1933 贝叶斯 bandit 思想。

## Problem Statement

带宽/路由选择的病：固定贪心锁死次优臂——**Beta 后验采样：每臂按 Beta(α,β) 抽一笔，取样本最大者**——探索随不确定性收缩。

## Solution

ThompsonSampler（core/experiment）：Beta(α=1+成功,β=1+失败) 后验采样选臂+select(种子化 Random)/armCount 读数+null/空臂/负计数 fail-fast+同种子同选择完全确定。

## Testing Decisions

手锚（全零历史均匀探索倾向+强偏历史收敛）；500 随机固定历史 vs 直接 Beta 采样分布合理性（选择频率单调于均值）+确定性；fail-fast。

## Out of Scope

- 不做多维/自适应参数面（单参数语义明示）。

## Further Notes

- 里程碑：V25/50。
