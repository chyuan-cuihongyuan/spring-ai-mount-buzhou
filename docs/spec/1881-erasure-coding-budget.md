# Spec 1881 — 纠删码冗余预算（effort #1881，R82）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2963–T2964，impl 1482）。借鉴：
> MinIO（50K+ 星）/Ceph 的纠删码 EC(k,m) 语义——k 数据分片 + m 校验
> 分片：可丢 m 片、修复读 k 片、容量可用率 k/(k+m)。冗余三读数事前
> 可算，存储方案评审有据。

## Problem Statement

多副本 vs 纠删码的容量账随手拍：EC(k,m) 配置的可用率、可容忍故障
数、修复读放大三件事分散在文档里——评审时没有统一计算面，副本数
与分片方案的对比缺口径。

## Solution

`ErasureCodingBudget`（core/policy，静态纯函数）：

- `usableRatio(data, parity)`：k/(k+m)——原始容量可用率（副本=1:1
  的 0.5 对照基准）；
- `tolerableFailures(data, parity)`：m——丢到 m 片仍可重建；
- `repairReads(data)`：k——单盘损坏修复需读的分片数（读放大口径）；
- `totalShards(data, parity)`：k+m——部署盘数下限。

## User Stories

1. 作为存储评审者，EC(4,2)：可用率 2/3、可丢 2 片、修复读 4——
   对比 3 副本（可用率 1/3、可丢 2）同一容忍度省 1/3 容量。
2. 作为容量规划者，1TB 原始容量 EC(8,4) → 667GB 可用——预算直算。
3. 作为运维者，repairReads=8 → 修复流量是坏盘数据的 8 倍——重建
   窗口网络压力有账。

## Implementation Decisions

- 纯计算零状态；data ≥ 1、parity ≥ 1 fail-fast（parity=0 无冗余
  非法——丢一片即数据丢失）。

## Testing Decisions

- EC(4,2) 四读数全断言（2/3、2、4、6）；EC(8,4) 可用率 2/3 修复
  读 8；副本对照 3 副本 = EC(1,2) 同容忍度容量对比；畸形三型
  fail-fast。

## Out of Scope

- 不做实际分片/重建（归存储层）；不做 Reed-Solomon 编解码。

## Further Notes

- 与 FailureDomainQuota（故障域配额）互补：那是故障域间备货，这是
  单域内冗余数学。
