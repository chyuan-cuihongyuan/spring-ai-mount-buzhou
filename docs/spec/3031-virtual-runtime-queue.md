# Spec 3031 — 虚拟运行时间公平队列（effort #3031，R32）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5063–T5064，impl 2032）。
> 借鉴：Linux CFS vruntime（按权折算的虚拟时钟公平）。

## Problem Statement

时间片轮转静态分片对异质实体僵化（重负载实体与轻负载同片——
轻者闲重者饿）；加权公平需要「按权重比例长期分得吞吐」的动态
口径，免周期重配置。

## Solution

`VirtualRuntimeQueue`（core/concurrent）：

- 实体记**虚拟运行时间**：pickNext(work) 恒取 vruntime 最小者
  （TreeMap O(log n)——CFS 红黑树同构），选中者 vruntime +=
  work/weight（重权者慢走）；
- 长期性质：等权退化为轮转；w 倍权得 w 倍配额；
- 并列取先入序（确定性可回放）；work=0 纯重选；id 唯一/
  weight ≥ 1 / work ≥ 0 校验；vruntimeOf 未注册 NaN。

## User Stories

1. 作为配额作者，租户/工具按权重长期公平——免静态分片。
2. 作为对账作者，vruntime 账面全读——公平性可审计可回放。

## Testing Decisions

- 等权三轮 9 择恰 "abcabcabc"；三倍权千择 750/250 ±60；空队列
  null；work10/w2 账面 +5（两步 +10）；并列先入先选（work 0 双
  择）；随机工作量等权均衡 0.5±0.05；重复 id/半权/null id/负
  work/未注册 NaN 六路 fail-fast。

## Out of Scope

- 不做新实体补偿（CFS 初始 min_vruntime 对齐留白——新注册从 0
  起，长跑后注册者会先获大份额直至追平——诚实口径）；不做
 抢占；不做并发。

## Further Notes

- 与 WeightedFairScheduler（DRR 赤字轮询）成公平双档：DRR 整数
  赤字定长轮、本件连续 vruntime 变长权——按口径精度选型。
- 里程碑：32/150。
