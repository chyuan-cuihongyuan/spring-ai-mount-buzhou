# Spec 9027 — Permutation Test 置换检验（effort #9027，W28）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9055–W9056，impl 2380）。
> 借鉴：Fisher 1930s 精确检验（scipy.stats.permutation_test 同源；原 bootstrap 置信区间已占（M 系）换替补）

## Problem Statement

t 检验正态假设失真、bootstrap 保分布
形态——**置换检验**：标签重排生成零假设
分布，均值差显著性无分布假设。

## Solution

PermutationTest（core/eval，静态纯函数面）：
twoSidedPValue(a,b,permutations,random)；
观测自身计入防 p=0。

## Testing Decisions

C(5,2)=10 全排列手锚（含镜像分组 p=0.2
精确）；同分布 p>0.3；强移位 p<0.001；
同种子逐值全等；fail-fast。

## Out of Scope

不做单侧（双侧明示）；不做统计量可插拔
（均值差明示）；不做精确枚举模式（蒙特
卡洛明示）。

## Further Notes

与 WilsonInterval 互补；与 bootstrap
（M 系）同族不同面。Wave 5 第四件。
