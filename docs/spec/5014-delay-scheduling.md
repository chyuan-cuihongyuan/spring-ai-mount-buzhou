# Spec 5014 — 延迟调度（effort #5014，S15）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6129–T6130，impl 2165）。
> 借鉴：Spark delay scheduling（locality wait——数据本地性等待预算）。

## Problem Statement

数据本地性调度的病：立即任意节点执行（远程拉数据——带宽
爆炸）或无限等待本地槽（本地槽不来——任务饿死）——**本地
性等待预算面**缺失。

## Solution

`DelayScheduling`（core/policy）：

- 每轮判定 `offer(preferred, preferredAvailable)`：首选可用 →
  `LAUNCH(preferred)`（零浪费）；不可用 → skips++ 继续 WAIT；
  skips 超 `maxSkips` 预算 → `LAUNCH(ANY)`（本地性换时效）
  并重置计数；
- 降级梯：PROCESS_LOCAL→NODE_LOCAL→RACK_LOCAL→ANY（枚举序
  ——等待中逐轮放宽）；
- 读数：skipsUsed；驱动确定性（无时间依赖——轮次即状态）；
- fail-fast：maxSkips<0、null level。

## User Stories

1. 作为数据本地性调度作者，预算内等本地、预算到就远执行——
   不饿死不浪费带宽。
2. 作为审计作者，同可用序列同裁决（确定性可回放）。

## Testing Decisions

- 首选可用零跳过即启；不可用 WAIT 累计到预算 → ANY 启动
  并重置；预算=0 立即 ANY；降级梯逐轮放宽；负预算/null
  fail-fast；确定性回放。

## Out of Scope

- 不做节点级资源画像；不做多任务全局装箱（slab 装箱件已
  覆盖装箱面）；不做推测执行联动（R27 已覆盖）。

## Further Notes

- 与 SpeculativeStragglerPolicy（R27 副本竞争）互补：本地性
  等待 vs 慢任务竞争。Wave 3 第四件。
- 里程碑：S15/50（30%）。
