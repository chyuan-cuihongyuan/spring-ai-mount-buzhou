# Spec 8030 — KMeansClustering（effort #8030，V31）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8061–V8062，impl 2332）。
> 借鉴：MacQueen 1967 K-Means 聚类思想（Lloyd 迭代）。

## Problem Statement

聚类的病：层次聚类 O(n³)——**Lloyd 迭代：指派-更新交替至收敛**，k-means++ 播种抑劣质初值。

## Solution

KMeansClustering（core/eval）：k-means++ 播种（种子化）+Lloyd 迭代（指派最近质心/质心=簇均值）至指派不变或 maxIter+assign 双面+空簇重播+维度一致/空集/k 越域 fail-fast+确定性。

## Testing Decisions

手锚（三团数据聚对）；惯性单调下降+同种子同结果；fail-fast。

## Out of Scope

- 不做在线增量/分布式面（单机批语义明示）。

## Further Notes

- 里程碑：V31/50。
