# Spec 2056 — 五数概括与 IQR 围栏（effort #2056，R57）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3213–T3214，impl 1607）。
> 借鉴：Tukey 箱线图/EDA——五点概括 + 1.5×IQR 离群围栏。

## Problem Statement

延迟/成本样本的分布描述与离群判定：均值±σ 假设正态且被离群点本身
污染（σ 越大离群越难检——自我掩蔽）；「多大算离群」需要不依赖分布
假设的稳健口径。

## Solution

`FiveNumberSummary`（core/metrics，纯函数零状态）：

- `of(samples)`：排序后五点 min/Q1/中位/Q3/max（R-7 线性分位插值）
  + IQR=Q3−Q1 + **Tukey 围栏**（Q1−1.5×IQR，Q3+1.5×IQR——默认内
  围栏；系数可自定义）；
- `Summary.isOutlier(value)`：围栏外即离群（稳健——四分位不被极端
  值拉动）；`iqr()` 读数；
- 单点退化（IQR=0 围栏=箱体）；契约：样本非空非 NaN、系数 ≥ 0
  fail-fast。

## User Stories

1. 作为延迟观测者，p99 被极端值拉爆时——IQR 围栏仍稳，离群点逐个
   显形。
2. 作为读数作者，五数一行交代全分布——比单均值信息量大一个量级。

## Testing Decisions

- 1..9 五点恰 1/3/5/7/9 与 IQR 4；偶数样本 R-7 插值（中位 2.5、Q1
  1.75、Q3 3.25）；围栏 [−3,13] 手算；极端高值离群/箱内不离/恰上界
  不离；单点退化；乱序入参同结果；自定义系数放宽围栏；畸形四型
  fail-fast。

## Out of Scope

- 不做外围栏（3×IQR——系数自定义覆盖）；不做箱线图渲染（纯数据）。

## Further Notes

- 与卡方/熵（2048/2050）同族：分布刻画三件——判定/量纲/五点+离群。
