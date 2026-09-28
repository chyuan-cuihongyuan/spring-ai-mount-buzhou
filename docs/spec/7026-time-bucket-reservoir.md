# Spec 7026 — TimeBucketReservoir 时间桶样本库（effort #7026，U27）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7253–U7254，impl 2278）。
> 借鉴：Dropwizard SlidingWindowReservoir 思想。

## Problem Statement

指标新鲜度的病：陈旧样本污染当前读数（只看全体不看
「最近 N 分钟」）——**时间戳入样+窗口快照面**缺失。

## Solution

`TimeBucketReservoir`（core/metrics）：record(value,nowNs)
入样（水位单调——回退 fail-fast）；snapshot(now,window)
窗口过滤（含边界、入库序——不排序诚实面）；evictBefore
显式物理清理；时间调用方注入（零真实时钟依赖完全确定）。

## Testing Decisions

- 窗口过滤手锚（50/150/1000ns 三档）；回退 fail-fast；
  清理计数；边界含；fail-fast。

## Out of Scope

- 不做分桶聚合面；不做自动后台清理。

## Further Notes

- 与 ReservoirSample（observability）同族不同面：随机抽样
  代表全体 vs 时间窗全量。
- 里程碑：U27/50（54%）。
