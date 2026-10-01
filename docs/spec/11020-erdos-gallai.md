# Spec 11020 — ErdosGallai 图序列可图化（effort #11020，Y21）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11041–Y11042，impl 2473）。
> 借鉴：Erdős–Gallai 1960 思想——NetworkX is_graphical 同源

## Problem Statement

度序列可图化判定——简单图存在性的不等式族面。

## Solution

ErdosGallai（core/concurrent）：isGraphical(int[])——降序副本校验：Σd 偶 + 每个 k∈[1,n]：Σ_{i≤k}d_i ≤ k(k−1)+Σ_{i>k}min(d_i,k)；负度 IllegalArgumentException（奇和非为布尔假不抛）。

## Testing Decisions

[2,2,2] 真手锚+[3,3,1] 奇和假+[2,2,0] 前缀不等式假手锚+200 随机序列（n=5，度 0..4）与 Havel–Hakimi 归约神像全等+确定性+fail-fast 五面。

## Out of Scope

不做重构边集输出（Havel–Hakimi 构造另立）；不做有向/重图域（简单图口径）。

## Further Notes

换静脉勘误入档（GomoryHu Gusfield 修正步记忆面未过全对真割圣像——单对 6≠7）；Wave 4 图结构族第三件。
