# Spec 10022 — CenterStarAligner（effort #10022，X23）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10045–X10046，impl 2425）。
> 借鉴：Center-Star 多序列比对（Terrain & Attimonelli 1994/ClustalW 前身形态）

## Problem Statement

多序列比对 n 维 profile DP 不可承受——选中心序列星形聚合的直接法：两两全局比对+中心空位列全列插空。

## Solution

CenterStarAligner（core/eval，静态纯函数面）：align(sequences)——中心选择（两两 NW 得分和最大平局取首序）+NW 全局比对回溯（对角→上→左确定平局序）+星形合并（中心空位列全列插空）+输出按输入序重排；投影契约（每行去空位还原原序列）+行等长契约；null/空集/空串 fail-fast。

## Testing Decisions

同序恒等+投影还原/行等长核验+比对收益不劣于未对齐+确定性+fail-fast 三面。

## Out of Scope

不做渐进树引导（ClustalW 另立）；不做迭代精化；不做扣分矩阵个性化（BLOSUM 另立）。

## Further Notes

与 NeedlemanWunsch（已占）不同面：MSA 聚合 vs 双序列；与 DeBruijnAssembler（10021）同族不同面：比对聚合 vs 图重构；开发勘误两处入档（回溯/合并残渣逻辑重写+输出行序中心优先改输入序重排）。
