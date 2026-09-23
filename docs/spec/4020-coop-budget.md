# Spec 4020 — tokio 协作预算（effort #4020，R21）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6041–T6042，impl 2121）。
> 借鉴：Tokio coop budget（异步运行时协作调度）。

## Problem Statement

单任务长循环不让出（无 IO 边界自觉 yield）饿死同 worker 其他
任务——「霸占」病缺运行时级根治件。

## Solution

`CoopBudget`（core/concurrent，纯预算账——真调度归运行时）：

- 任务每次被调度获得固定 N 点预算；关键路径操作 charge 扣 1；
- 预算尽 hasBudget()=false（让出时机），尽后再扣 fail-fast
 （让出检查缺失即红）；yield() 重置满额（重新排队后继续）；
- 每任务独立账户（构造期定 initial，读数可审计）。

## User Stories

1. 作为运行时作者，让出点由关键操作自然分布——业务代码无需
   自觉 yield。
2. 作为公平性审计者，预算扣减与重置可回放。

## Testing Decisions

- 3 点扣至恰尽；8 点尽后 yield 重置满额；尽后再扣
  IllegalStateException + 重置后恢复；双任务账户互不串门；
  畸形三型 fail-fast（0/负预算）。

## Out of Scope

- 不做真调度/排队（归运行时）；不做预算透支；不做按操作类别
  加权扣减（单位点口径）。

## Further Notes

- 与 SpawnGate（准入）互补：彼管「进不进」、本管「进了之后
  霸不霸」。
- 里程碑：21/50。
