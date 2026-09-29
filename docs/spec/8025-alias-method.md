# Spec 8025 — AliasMethod（effort #8025，V26）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8051–V8052，impl 2327）。
> 借鉴：Walker 1977/Vose 1991 别名法 O(1) 加权采样思想。

## Problem Statement

加权采样的病：逐轮扫权重 O(n) 或预构造前缀+二分 O(log n)——**别名法预处理 O(n) 后每采样 O(1)**（桶+别名双掷）。

## Solution

AliasMethod（core/experiment）：of(weights,种子) 建桶（概率缩放 1.0 域+亏空/溢出两队列 Vose 装桶）+sample O(1)+weights 读数+负权/全零/空 fail-fast+同种子同序列可回放。

## Testing Decisions

手锚（[1,1,1,1] 均匀/[4,1] 桶形）；500 随机大样频率 vs 归一权重偏差 <3%+同种子同序列；fail-fast。

## Out of Scope

- 不做多维/自适应参数面（单参数语义明示）。

## Further Notes

- 里程碑：V26/50。
