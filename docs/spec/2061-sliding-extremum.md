# Spec 2061 — 单调队列滑窗极值（effort #2061，R62）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3223–T3224，impl 1612）。
> 借鉴：单调双端队列经典算法——定长滑窗极值 O(1) 摊销。

## Problem Statement

定长滑窗内最大/最小（尖峰检测 / 谷底配额）：朴素每次重扫窗 O(W)；
堆删除懒惰化 O(log n) 且存全窗——单调队列 O(1) 摊销且队内只存
「潜在未来极值」候选。

## Solution

`SlidingExtremum`（core/metrics，synchronized 小临界区）：

- `offer(value)`：入队前弹掉**永无出头之日**的队尾（求 max：≤ 新
  者全弹——更老且不大，滑出后也轮不到）；返回当前窗极值（队首）；
- 序号自滑出（队首 seq ≤ 当前 seq − window 即过期弹）——无需显式
  窗口数组；
- max/min 双口径（构造布尔）；空窗 current()=NaN；size=候选数
 （被压制弹出者不在内）；
- 契约：window ≥ 1、value 有限非 NaN fail-fast。

## User Stories

1. 作为尖峰检测作者，滑动最大延迟 O(1) 摊销——万点流不重扫。
2. 作为配额作者，min 口径滑动谷底——低位窗口显形。

## Testing Decisions

- max 六步窗滑（3 未过期→滑出→新 5 压制）；min 镜像；递增流逐个压
  制 size=1；递减流 max 恒新鲜；空窗 NaN；畸形三型 fail-fast。

## Out of Scope

- 不做双端同时（一件一口径）；不接告警链（接线归后续轮）。

## Further Notes

- 与 MinRttTracker（全窗最小基线）/BoundedTopK（全局榜单）互补：
  滑动窗极值流。
