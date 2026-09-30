# Spec 9040 — Bitonic Sorter 双调排序网络（effort #9040，W41）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9081–W9082，impl 2393）。
> 借鉴：Batcher 1968 双调网络（GPU 并行排序/排序网络理论同源——数据无关固定模式）

## Problem Statement

快排对敌对数据退化 O(n²)、依赖序数据
分支预测失效——**双调网络**：数据无关
固定比较模式，可全并行铺硬件。

## Solution

BitonicSorter（core/metrics，静态纯函数面）：
sort(values) 原地（n=2^k 契约）；
comparatorCount 同构递归计数；sortedCopy。

## Testing Decisions

8 元素双调手锚；退化三态；2^1..2^12
随机 vs Arrays.sort 全等；比较器真值锚
1/6/24/80；fail-fast。

## Out of Scope

不做任意 n 变体（2 的幂契约明示）；
不做奇偶转置网络另立；不做并行执行
（固定模式面——执行归消费方）。

## Further Notes

与 RadixSorter 同域不同面：
非比较 vs 固定模式比较网络。开发勘误：
comparatorCount 闭式臆造证伪——改同构
递归（Batcher 真值）。Wave 7 收束件。
