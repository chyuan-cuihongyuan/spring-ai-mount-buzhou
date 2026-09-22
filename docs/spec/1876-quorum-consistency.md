# Spec 1876 — 法定人数一致性（effort #1876，R77）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2953–T2954，impl 1477）。借鉴：
> Dynamo/Cassandra（数万星）的 quorum 读写交集语义——R+W>N 则任意读集与
> 写集必有公共副本，读到最新写有数学保证；交集数与可容忍故障数可预算。

## Problem Statement

多副本读写配置（N 副本、读 R 份、写 W 份）随手拍：拍弱了读到旧值
（R+W ≤ N 时读集写集可不相交），拍强了写一个挂俩就停摆——一致性
等级、持久性、可用性余量、读写交集 guarantee 四件事没有统一计算面。

## Solution

`QuorumConsistency`（core/transaction，静态纯函数）：

- `strongConsistency(N, R, W)`：R+W>N 判定——真则任意读集含最新写；
- `overlapCount(N, R, W)`：R+W−N 与 0 取大——任意读集与写集的保证
  公共副本数（0 = 交集无保证——弱一致）；
- `tolerableWriteFailures(N, W)`：N−W——写法定人数下还能收写的副本
  故障数；`tolerableReadFailures(N, R)`：N−R 同理；
- `consistentAvailability(N, R, W)`：min(两余量)——读写一致性同时保住的
  最大故障副本数。

## User Stories

1. 作为存储配置作者，(3,2,2)：R+W=4>3 强一致、交集 1、可挂 1 副本
   ——配置前就有账。
2. 作为架构评审者，(3,1,1)：R+W=2≤3 弱一致一目了然——争议止于判定。
3. 作为容量规划者，W>N/2 是写多数派持久性底线——低于即警。

## Implementation Decisions

- 纯计算零状态；布尔/整数四函数各司其职；N≥1、1≤R≤N、1≤W≤N
  越界 fail-fast（IllegalArgumentException 带修法）。

## Testing Decisions

- 经典 (3,2,2) 强一致四读数全断言；(5,3,3) 交集 1 + 可挂 1；
  (3,1,1) 弱一致 + 交集 0；(4,3,2) 边界 R+W=5>4 强、写余 2 读余 1；
  畸形五型 fail-fast（N=0 / R=0 / R>N / W=0 / W>N）。

## Out of Scope

- 不做副本选择与读修复（归存储层）；不做动态调参建议。

## Further Notes

- 与 VectorClockCompare（偏序）互补：那是版本比较，这是配置判定。
