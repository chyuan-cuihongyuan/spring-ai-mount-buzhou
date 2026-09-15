# Spec 1708 — 租约续期抖动读面（effort #1708，R9）

> wayfinder map：`.wayfinder/maps/effort-1700.md`（T2617–T2618，impl 1308）。借鉴：
> etcd lease keepalive / ZooKeeper session 的续期节奏健康——「续上了」不等于
> 「续得稳」。

## Problem Statement

SessionLeaseStore 族续租成功与否有账，但**续期节奏**无读数：续租间隔忽长
忽短 = GC 停顿/锁竞争/调度饥荒的前兆，等 lease 真丢才显形就晚了。

## Solution

`LeaseRenewalStats`（core/session，静态纯函数）：`analyze(intervalsMillis)`
→ `RenewalReport(samples/meanMillis/cv/maxSkewMillis)`。cv = 总体标准差/
均值（变异系数，无量纲）；maxSkew = max−min；负值样本忽略；n<2 哨兵
cv=−1、maxSkew=−1。

## User Stories

1. 作为平台运维，cv 从 0.05 漂到 0.6 → 续租线程被什么卡了，先于租约丢失显形。
2. 作为平台运维，maxSkewMillis=8s → 最坏间隔离 TTL 多远一目了然。

## Implementation Decisions

- 纯读面：不吃 lease 对象，只吃间隔数（宿主从续期循环埋点喂入）。
- 总体方差（除 n）——节奏抖动关注相对散布而非样本估计。

## Testing Decisions

- 空/单样本哨兵；恒定节奏 cv≈0 且 skew=0；抖动节奏 cv>0.3 且 skew=8s；
  负值忽略。

## Out of Scope

- 不做 TTL 剩余量联动/不自动续期；不做滑动窗（序列由宿主裁剪）。

## Further Notes

- 租约三面：契约（AbstractBuzhouStoresContractTest）+ 统计面（本轮）+
  对账（既有 LSessionLedgerAuditTest 域外）。
