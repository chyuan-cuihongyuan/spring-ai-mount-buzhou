# Spec 6027 — Stride Scheduler 步幅调度（effort #6027，T28）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6255–T6256，impl 2228）。
> 借鉴：MIT exokernel stride 思想。源码于 T24 核账批预入档。

## Problem Statement

比例份额调度的病：WRR 短窗倾斜（权重比在任意前缀不成立）
与彩票调度随机（同输入不同序，不可回放）——**确定性比例
份额面**缺失。

## Solution

`StrideScheduler`（core/concurrent，源码已预载）：

- 客户端按票数取步幅 stride = BASE/tickets；每被调度一次
  pass += stride；serve 恒选 pass 最小者（并列 id 小——
  完全确定性）；长程 served 次数精确∝票数；
- 全客户端扫描选最小（调度面客户端数小——O(n) 简洁换
  正确）；remove/passOf/ticketsOf 读数；
- fail-fast：票数≤0、重复注册、缺席操作、空调度。

## User Stories

1. 作为配额作者，多租户配额按票数精确比例——份额可证。
2. 作为审计作者，同注册同出序——调度确定性可回放。

## Testing Decisions

- 票数 {1,1,2} 400 次调度恰 {100,100,200}（短窗精确）；
  两客户端严格交替；同注册双实例序列全等；remove 排除；
  fail-fast。

## Out of Scope

- 不做 O(log n) 堆选最小（客户端数小）；不做动态改票
 （重注册面）。

## Further Notes

- 与 WeightedRoundRobin（5026）同族不同面：平滑插值轮询
  vs 单调 pass 记账；与 DeficitRoundRobin（5027）不同面：
  字节亏空 vs 票数比例。
- 里程碑：T28/50（56%）。
