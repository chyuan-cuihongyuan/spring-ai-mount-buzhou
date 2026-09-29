# Spec 8049 — SkylineProblem 天际线轮廓（effort #8049，V49）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8099–V8100，impl 2351）。
> 借鉴：天际线问题经典（sweep line 离散事件——城市轮廓 LC218 同源）。

## Problem Statement

轮廓合并的病：逐对建筑两两求并 O(n²)——**离散事件扫线：
左端+高度入堆、右端出堆，关键高度变化才落点 O(n log n)**。

## Solution

SkylineProblem（core/metrics，静态工具面）：of(buildings
[left,right,height]，left<right、height>0 越域 fail-fast）+
skyline 返回关键点序列 [x,height]（含终尾归零点）+堆
（延迟删除 TreeMap 计数）+确定性（同 x 取序固定）。

## Testing Decisions

- 经典手锚（LC218 五楼轮廓逐点）；单楼/相邻/包含/重叠
  多重手锚；确定性双跑；fail-fast。

## Out of Scope

- 不做流式增量；不做浮点坐标（整型域明示）。

## Further Notes

- 与 SweepLineIntervals（3013）同族不同面：区间并发峰值
  计数 vs 轮廓关键点几何。
- 里程碑：V49/50（98%）。
