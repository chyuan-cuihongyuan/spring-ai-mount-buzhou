# Spec 9046 — Branch and Bound 分支限界背包（effort #9046，W47）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9093–W9094，impl 2399）。
> 借鉴：Branch and bound（Land-Doig 1960——运筹学/IP 求解器同源框架）

## Problem Statement

全子集枚举 O(2ⁿ) 稍大即爆——**分支限界**：
LP 松弛上界剪枝，DFS 精确解。

## Solution

BranchAndBound（core/policy，静态纯函数面）：
knapsack(weights,values,capacity)——密度序
+贪心分数上界剪枝。

## Testing Decisions

经典锚逐项枚举；120 随机实例 vs 全子集
对拍全等；确定性；fail-fast。

## Out of Scope

不做物品选择集回读（值面）；不做
多维背包/完全背包；不做分支策略可插拔。

## Further Notes

与 BinPackBalance 同域不同面；
与 TspTwoOpt（9045）同根不同面。
Wave 8 收束件。
