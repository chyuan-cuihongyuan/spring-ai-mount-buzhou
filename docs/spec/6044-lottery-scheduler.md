# Spec 6044 — Lottery Scheduler 彩票调度（effort #6044，T44）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6287–T6288，impl 2244）。
> 借鉴：Waldspurger 彩票调度思想。源码于对账补账批预入档。

## Problem Statement

加权调度的病：WRR 短窗倾斜（任意前缀内比例不成立）与
固定轮询（无权重语义）——**按票数比例的加权随机面**缺失。

## Solution

`LotteryScheduler`（core/concurrent，源码已预载）：

- 客户端持 n 张票，每次抽取以 n/总票数概率获胜；长程
  期望比例精确∝票数，短窗随机（并发友好——无需全局顺序）；
- 种子化 Random（同种子同序列——确定性可回放）；
- register/remove（缺席 fail-fast）+clientCount/totalTickets/
  ticketsOf 读数；票数≤0/重复注册/空池 fail-fast。

## User Stories

1. 作为调度作者，按权重随机分配资源且无需全局排序。
2. 作为审计作者，同种子双实例序列全等——行为可回放。

## Testing Decisions

- {1,3} 票 4000 抽比例收敛 ±10%；同种子序列全等；注销后
  剩余客户端全胜+读数归位；票数 0/重复/缺席 fail-fast。

## Out of Scope

- 不做彩票转让（transfer）；不做并发安全（单线程决策面）。

## Further Notes

- 与 StrideScheduler（6027）同族不同面：确定性步幅记账 vs
  随机彩票（期望同比例）；与 WeightedReservoirSampler 不同面：
  流采样 vs 调度选择。
- 里程碑：T44/50（88%）。
