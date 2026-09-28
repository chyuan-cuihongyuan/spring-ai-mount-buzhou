# Spec 7042 — TernarySearch 三分搜索（effort #7042，U43）

> wayfinder map：`.wayfinder/maps/effort-7000.md`（U7285–U7286，impl 2294）。
> 借鉴：单峰函数三分求极值思想（凸优化同源）。

## Problem Statement

单峰极值的病：网格扫描以精度换算力（ε 粒度爆炸）与
牛顿法需导数+多峰陷阱——**双探点舍 1/3 区间面**缺失。

## Solution

`TernarySearch`（core/metrics，静态工具面）：每次迭代
m1/m2 双探点比较、舍去不含极值的 1/3 区间，O(log((r−l)/ε))；
maximize/minimize 对称面；单峰假设明示（多峰不承诺
全局极值——诚实边界）；区间倒置/eps≤0 fail-fast。

## Testing Decisions

- 抛物线顶点/谷底手锚（±1e-3 容差）；ε 收紧精化单调；
  fail-fast 四路。

## Out of Scope

- 不做多峰全局搜索；不做整数域三分。

## Further Notes

- 与 InterpolationSearch（6022）同族不同面：离散存在性
  定位 vs 连续单峰极值。
- 里程碑：U43/50（86%）。
