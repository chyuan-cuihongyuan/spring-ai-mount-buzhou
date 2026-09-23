# Spec 5018 — Read Repair 读修复（effort #5018，S19）

> wayfinder map：`.wayfinder/maps/effort-5000.md`（T6137–T6138，impl 2169）。
> 借鉴：Dynamo/Cassandra read repair（读时对账 + 陈旧副本回写）。

## Problem Statement

多副本读的病：读到旧版本不处理（读放大撕裂持续）或全量
比对（每次读都全副本往返）——**读时择优 + 陈旧清单回写面**
缺失。

## Solution

`ReadRepair`（core/transaction）：

- `stitch(reports)`：N 副本版本汇报——取**最高版本**者为胜者
 （读返回值）；版本低于胜者者为陈旧清单（按副本名字典序
  确定性排序）——调用方对其回写胜者值；
- 版本并列：副本名字典序最小者胜（确定性 tie-break）；
- 读数面：Stitch(winner, staleReplicas)；
- fail-fast：空汇报 ISE、null 副本名/负版本 IAE。

## User Stories

1. 作为多副本读作者，读到新版本顺手把旧副本修回来——撕裂
   不持续。
2. 作为审计作者，同汇报同裁决（确定性可回放）。

## Testing Decisions

- 一致汇报（空 stale）；分叉（max 版本胜 + stale 排序）；
  版本并列字典序 tie-break；空汇报/负版本/null fail-fast；
  确定性回放。

## Out of Scope

- 不做向量版本合并（多值 sibling 合并——VectorClockOrder 已
  覆盖因果面）；不做回写传输本身；不做反熵全量对账。

## Further Notes

- 与 HintedHandoff（S11）同族不同面：读路径修复 vs 投递
  暂代。Wave 4（对账执行族）开波。
- 里程碑：S19/50（38%）。
