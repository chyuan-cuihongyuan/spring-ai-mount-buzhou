# Spec 1888 — 连接池容量启发（effort #1888，R89）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2977–T2978，impl 1489）。借鉴：
> HikariCP（20K+ 星）wiki 的池容量公式——pool size = cores×2 +
> effective_spindles：「比这更多只会更慢」的反直觉结论（连接数超过
> 线程可驱动数只增加上下文切换与锁争用）。

## Problem Statement

连接池大小靠「越大越好」的直觉拍：池过大反而吞吐下降（上下文切换
+ 锁争用 + 数据库端内存），过小又饿等——单节点公式与多节点预算
拆分没有统一计算面。

## Solution

`PoolSizeHeuristic`（core/concurrent，静态纯函数）：

- `optimalSize(cores, spindles)`：cores×2 + spindles——单节点池容量
  启发（SSD/纯网络存储 spindles≈0）；
- `splitBudget(total, nodes)`：全局连接预算按节点均衡拆分——前
  remainder 个节点 +1（余数摊平，不偏科）；
- `saturationRatio(active, size)`：饱和度读数（≥0.9 预警候选）。

## User Stories

1. 作为容量作者，4 核 + 2 磁轴 → 10 连接——公式一句话有据。
2. 作为多实例部署者，全局预算 23 拆 4 节点 → {6,6,6,5}——余数
   摊平不偏科。
3. 作为运维者，饱和度 0.95 → 扩容或公式复审的信号。

## Implementation Decisions

- 纯计算零状态；cores ≥ 1、spindles ≥ 0、nodes ≥ 1、预算 ≥ 节点数
  （每节点至少 1 连接）fail-fast；size=0 哨兵 0.0。

## Testing Decisions

- 经典 4+2→10；SSD 4+0→8；拆分 23/4→{6,6,6,5}；饱和度 19/20=0.95
  与零池哨兵；畸形四型 fail-fast。

## Out of Scope

- 不做真实连接生命周期管理（归 DataSource）；不做动态调参。

## Further Notes

- 与 GradientAdaptiveLimiter（并发上限自适应）互补：那是运行时
  反馈调优，这是部署期静态基线。
