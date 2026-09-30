# Spec 10020 — NussinovFolder（effort #10020，X21）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10041–X10042，impl 2423）。
> 借鉴：Nussinov–Jacobson 折叠（Nussinov 1980——ViennaRNA 同源）

## Problem Statement

RNA 二级结构预测需无假结最大配对——区间 DP 四向递推+回溯产出配对集。

## Solution

NussinovFolder（core/concurrent，静态纯函数面）：fold(sequence)——D[i][j] 四向递推（不配/单配/双分裂）+回溯无假结配对集；Watson–Crick+GU 摆动计分；最小发夹环长 3；非 ACGU/长度不足 fail-fast。

## Testing Decisions

发夹手锚 GGGAAACCC=3 对+无配序列 0 对+无假结性质两两核验+最小环长核验+计数/配对集自洽+fail-fast 四面。

## Out of Scope

不做能量模型（Zuker 另立）；不做假结（non-crossing 契约）；不做共变基多序列协同折叠。

## Further Notes

与 NeedlemanWunsch（已占）同 DP 家族异面：区间折叠 vs 线性对齐。
