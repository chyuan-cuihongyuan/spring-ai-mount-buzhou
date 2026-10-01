# Spec 10034 — DominatorTree Lengauer–Tarjan 支配树（effort #10034，X35）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10069–X10070，impl 2437）。
> 借鉴：Cooper–Harvey–Kennedy 2001 思想——LLVM 同源（LT 1979 勘误换面，见 Notes）

## Problem Statement

CFG 支配关系——编译器优化/ SSA 构造的必经点树面。

## Solution

DominatorTree（core/concurrent）：immediateDominators(n,edges,root)——逆后序迭代数据流+intersect idom 链双指爬升收敛（CHK 面）；idom[root]=root、不可达 −1 哨兵；null 边/越界 fail-fast。

## Testing Decisions

含回边 CFG 手锚（菱形 join 支配收敛）+链图逐级手锚+不可达 −1+20 随机图与暴力删除法（删点判可达）全等圣像（Python 对拍 3000 图先行实证）+确定性+fail-fast 四面。

## Out of Scope

不做后支配树（逆图镜像另立）；不做增量更新面（动态 CFG 另立）；不做 SSA 构造消费面。

## Further Notes

勘误入档：Lengauer–Tarjan 1979 半支配点伪码记忆面未过暴力删除法对拍神像（3000 随机图 610 红——两种桶清算变体皆红）——换 CHK 同题异面后 0/3000 全绿——「不可自洽即换静脉」纪律；Wave 6 图结构进阶族收束件。
