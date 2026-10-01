# Spec 10048 — JumpPointSearch 跳点搜索网格寻路（effort #10048，X49）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10097–X10098，impl 2451）。
> 借鉴：Harabor–Grastien 2011 思想——PathFinding.js/GameAIPro 同源（AStarSearch 已占异面：任意图 vs 均匀格加速）

## Problem Statement

均匀格寻路的对称路径爆炸——跳点剪枝的最优等价加速面（A* 同根异面）。

## Solution

JumpPointSearch（core/concurrent）：path(boolean[][],int[2],int[2])——跳点识别：直向扫描遇可走强制邻格或撞墙回退即跳点、斜向扫描递归直扫；跳点集上 A*（octile 启式+开放堆 (f,r,c) 确定序）；回溯跳点链逐格展开全路径。

## Testing Decisions

直线走廊起终点手锚+L 形绕障手锚+切角允许面+20 随机格与网格 Dijkstra（octile 权切角允许）总代价相等圣像+不可达空列+确定性+fail-fast 五面。

## Out of Scope

不做 any-angle 变体（Theta* 另立）；不做动态障碍增量面；不做权重格（均匀代价域）。

## Further Notes

A*（W49）同根不同面：均匀格跳点剪枝 vs 任意图启发式——X49 独件；开发勘误两处入档：禁切角域强制邻格双链复杂度超时预算换原 paper 切角允许口径+起点无剪枝预播种漏配死路面。
