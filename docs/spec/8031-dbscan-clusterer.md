# Spec 8031 — DbScanClusterer（effort #8031，V32）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8063–V8064，impl 2333）。
> 借鉴：Ester 1996 DBSCAN 密度聚类思想。

## Problem Statement

聚类的病：K-Means 强设凸球簇且需预知 k——**DBSCAN 密度可达：ε 邻域+minPts，任意形状+噪声标记**。

## Solution

DbScanClusterer（core/eval）：of(points,eps,minPts)+cluster 返回标签数组（-1 噪声）+扩张队列种子化顺序+ε≤0/minPts<1 fail-fast+确定性。

## Testing Decisions

双月牙/团+离群点手锚；边界点归属确定性；fail-fast。

## Out of Scope

- 不做在线增量/分布式面（单机批语义明示）。

## Further Notes

- 里程碑：V32/50。
