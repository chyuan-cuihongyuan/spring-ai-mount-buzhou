# Spec 6047 — PowerOfTwoChoices 二择一负载均衡（effort #6047，T47）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6295–T6294，impl 2247）。
> 借鉴：Mitzenmacher「The Power of Two Random Choices」思想（Wave 7 遗珠补位）。

## Problem Statement

负载放置的病：单次随机哈希长尾热桶（尾延迟放大）与
全局最小扫描（每次放置 O(n)）——**O(1) 二样本地比较面**缺失。

## Solution

`PowerOfTwoChoices`（core/policy）：

- 每次放置随机抽两个不同桶（同桶重抽——无放回二择），
  取装载较小者；并列取下标小者（完全确定无随机残余）；
- 最大装载从单次随机 O(ln n/ln ln n) 压到 O(ln ln n)
  （非对称性引理）；
- 种子化 Random（同种子同放置序列——确定性可回放）+
  place/loadOf/maxLoad/binCount/placed 读数；桶数越域/
  下标越域 fail-fast。

## User Stories

1. 作为网关作者，O(1) 放置即可压平长尾热桶。
2. 作为审计作者，1M 球 1M 桶 maxLoad≤4 可复现钉住。

## Testing Decisions

- 2 桶差距始终 ≤1（严格放轻者）；1M 球 1M 桶 maxLoad≤4
  （单次随机典型 ≥6）；放置守恒；同种子序列全等；fail-fast。

## Out of Scope

- 不做摘除与再均衡（不可变负载增长面）；不做权重。

## Further Notes

- 勘误：并列判定初版 loads[i]<=loads[j] 取 min(i,j) 可
  越过严格更轻桶——拆三支修正；同桶重抽缺失（i==j 等效
  单次随机）一并钉住修正。
- 与 ConsistentHashRing（同包）同族不同面：键映射粘滞 vs
  负载感知放置；与 TwoChoiceSelector（3022）同族不同面
  （T48 补登）：纯函数单次决策（负载快照由调用方维护，
  全等负载退化均匀随机）vs 有状态放置账本（桶内装载自
  维护+并列取小下标无随机残余+守恒/最大装载审计面）。
- 里程碑：T47/50（94%）。
