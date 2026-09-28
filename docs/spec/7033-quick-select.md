# Spec 7033 — QuickSelect 确定性选择（effort #7033，U34）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7267–U7268，impl 2285）。
> 借鉴：Blum-Floyd-Pratt-Rivest-Tarjan 1973 中位数的中位数思想。

## Problem Statement

第 k 小选择的病：排序取第 k O(n log n)（放大）或随机
枢轴最坏 O(n²)（对抗输入）——**五数分组确定性枢轴
O(n) 最坏保证面**缺失。

## Solution

`QuickSelect`（core/concurrent，静态工具面）：三路分区
（< = >）+中位数的中位数枢轴递归；副本语义（原数组
不动）；并列秩取值域（第 k 小允许多解之一）；k∈[0,n)
越域 fail-fast；无随机完全确定。

## Testing Decisions

- 200 随机序列全 k vs 排序圣像逐位全等+原数组不动断言；
  重复值域钉住；双跑确定性；fail-fast。

## Out of Scope

- 不做 TopK 多选；不做流式中位数（MedianKeeper 面）。

## Further Notes

- 与 InterpolationSearch 同族不同面：存在性定位 vs 秩
  选择。
- 里程碑：U34/50（68%）。
