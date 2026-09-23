# Spec 4036 — 标量卡尔曼滤波（effort #4036，R37）

> wayfinder map：`.wayfinder/maps/effort-4000.md`（T6073–T6074，impl 2137）。
> 借鉴：Kalman 标量滤波（预测/更新 + 增益闭环）。

## Problem Statement

单指标在线估计的病：裸读数噪声直用（抖动惊弓）或朴素滑动
平均（对动态信号滞后死板、无不确定性度量）——**带不确定
性闭环的最优融合面**缺失。

## Solution

`ScalarKalmanFilter`（core/metrics）：

- 预测：常值模型状态外推不变、方差增长 `p += q`（过程噪声
  ——不确定性随时间累积）；
- 更新：增益 `k = p/(p+r)`（测量噪声 r>0），状态融合
  `x += k(z−x)`、方差收缩 `p *= (1−k)`——增益随确定性
  升高单调下降（不信新读数），predict 后方差回升（重新
  信任新读数）——闭环自适应；
- 读数面：state()/variance()/lastGain()；
- fail-fast：r≤0 / p0≤0 / q<0 / 非有限读数。

## User Stories

1. 作为指标估计作者，抖动被平滑、突变被追踪——增益自适应。
2. 作为审计作者，同观测序列同估计轨迹（确定性可回放）。

## Testing Decisions

- 常值信号收敛（0→42 收敛近真值、方差单调收缩）；增益纯
  update 序列单调下降 / predict 后回升双证；平滑降噪
 （滤波方差 < 裸读数方差）；确定性回放；畸形定构与
  非有限读数 fail-fast。

## Out of Scope

- 不做多维状态/控制矩阵（标量口径）；不做 RTS 平滑
 （离线面）；不做自适应噪声估计（q/r 定构给定）。

## Further Notes

- 与 EWMA（DynamicSnitchPenalty α 平滑）同族不同面：EWMA
  无不确定性模型恒定权重 vs 卡尔曼增益随方差闭环自适应。
  Wave 7（时序事务族）开波。
- 里程碑：37/50。
