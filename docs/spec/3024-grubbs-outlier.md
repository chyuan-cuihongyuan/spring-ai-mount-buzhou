# Spec 3024 — Grubbs 离群检验（effort #3024，R25）

> wayfinder map：`.wayfinder/maps/effort-3000.md`（T5049–T5050，impl 2025）。
> 借鉴：Grubbs 1950（极端学生化偏差 ESD）。

## Problem Statement

单点毛刺（一次 30s 延迟/一笔异常成本/一个跑分野值）与正常波动
的区分缺统计口径：拍阈值误报漏报两头错；IQR 围栏抗多离群但对
**单**离群功效偏低。

## Solution

`GrubbsOutlier`（core/eval，纯函数静态件）：

- G = max|xᵢ−mean| / s（样本标准差）对**内置双侧 α=0.05 临界值
  表**（n=3..32，标准 Grubbs 表——卡方检验同款表制先例）；
- `Result(mean, stdDev, statistic, criticalValue, outlier,
  outlierDetected)`——离群值只在检出时非空（诚实 null）；
- 矩计算复用 WelfordAccumulator；零方差（全等样本）诚实拒绝；
  表界外 fail-fast；表长静态自证断言。

## User Stories

1. 作为评估作者，单点毛刺统计判定——不拍阈值。
2. 作为对账作者，G/临界/矩全读数可复算。

## Testing Decisions

- 高/低双侧明显离群检出（outlier 值对）；干净六点样本不误报
  （outlier null）；{1,2,3,4,100} 手算 mean=22、s=√1902.5、
  G≈1.7879 超 n=5 临界 1.672；零方差拒绝；n<3/n>32/null 三路
  fail-fast；临界值随 n 严格增（表位 sanity）。

## Out of Scope

- 不做多离群迭代（ESD-Max/GESD 与 IQR 自适应留白——本件单离群
  口径诚实声明）；不做 α 参数化（0.05 固定表）；不做正态性前置
  检验（归调用方）。

## Further Notes

- 与 FiveNumberSummary IQR 围栏互补：单离群正态假设 Grubbs 功效
  最优；多离群互掩场景归 IQR。
- 里程碑：25/150。
