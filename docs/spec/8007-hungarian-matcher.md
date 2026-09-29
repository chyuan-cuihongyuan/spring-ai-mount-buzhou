# Spec 8007 — HungarianMatcher 指派匹配（effort #8007，V8）

> wayfinder map：`.wayfinder/maps/effort-8000.md`（V8015–V8016，impl 2309）。
> 借鉴：Kuhn 1955 匈牙利算法（Munkres 1957 改良；任务指派/推荐配对同源）。

## Problem Statement

任务指派的病：全排列枚举 O(n!) 不可承受，贪心逐行取最小
**局部最优锁死全局**（后行被迫付高价）。

## Solution

`HungarianMatcher`（core/policy，静态工具面）：O(n³) 位势
法（对偶 u/v 调整 + 增广路交替树）——`minCost` 最小总代价
+`assignment` 行→列指派（行序返回）；方阵语义（非方阵/
空阵/null fail-fast）；确定性纯函数（同阵同指派，位势
调整按最小未覆盖值固定步长——并列代价取最小列标，明示）。

## Testing Decisions

- 手锚（[[4,1,3],[2,0,5],[3,2,2]]→5；对角阵/全等阵并列
  canonical）；100 随机 n≤6 vs 全排列暴力圣像逐值全等；
  指派合法性（行/列双射）性质钉住；fail-fast。

## Out of Scope

- 不做最大权变体（取负即得——明示）；不做增量增广。

## Further Notes

- 与 ActivitySelectionGreedy（7045）同族不同面：区间兼容
  贪心 vs 指派全局最优对偶调整。
- 里程碑：V8/50（16%）。
