# Spec 8032 — PowerIteration（effort #8032，V33）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8065–V8066，impl 2334）。
> 借鉴：Page & Brin 1998 PageRank 幂迭代思想。

## Problem Statement

主特征向量的病：特征分解 O(n³)——**幂迭代：反复 x←Ax/‖Ax‖ 收敛至主特征向量**（谱半径优势）。

## Solution

PowerIteration（core/eval）：of(列随机矩阵或对称阵,maxIter,tol)+iterate 收敛主向量+特征值 Rayleigh 商读数+非方阵/维度不一致/负容差 fail-fast+确定性。

## Testing Decisions

已知主特征向量小阵手锚 ±1e-6；收敛单调；fail-fast。

## Out of Scope

- 不做在线增量/分布式面（单机批语义明示）。

## Further Notes

- 里程碑：V33/50。
