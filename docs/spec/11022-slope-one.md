# Spec 11022 — SlopeOne 协同过滤（effort #11022，Y23）

> wayfinder map：`.wayfinder/maps/effort-11000.md`（Y11045–Y11046，impl 2475）。
> 借鉴：Lemire–Maclachlan 2005 思想——Slope One 同源

## Problem Statement

评分矩阵的线性偏离填充——最简协同过滤预测面。

## Solution

SlopeOne（core/metrics）：predict(double[][],int,int)——对用户已评项 j 且与 item 有共评者：dev_ji=Σ(r_kj−r_ki)/freq、pred=(Σ(dev_ji+r_uj)freq_ji)/(Σfreq_ji)。

## Testing Decisions

3 用户×3 物手锚（手算偏离矩阵互证 1e-9）+已评项恒等面（预测=原评分）+无共评基座 IllegalArgumentException+确定性+fail-fast 五面。

## Out of Scope

不做加权 Slope One（权重变体另立）；不做增量偏离矩阵维护面（在线域另立）。

## Further Notes

最简协同过滤原语——推荐谱系锚定件；Wave 4 评分与相关性族首件。
