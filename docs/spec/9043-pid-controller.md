# Spec 9043 — PID Controller 比例积分微分控制（effort #9043，W44）

> wayfinder map：`.wayfinder/maps/effort-9000.md`（W9087–W9088，impl 2396）。
> 借鉴：PID（Minorsky 1922——工业控制百年默认件，K8s 控制环同源）

## Problem Statement

P-only 稳态余差不散、手调阈值无收敛语义——
**PID**：误差驱动闭环，比例/积分/
微分三作用。

## Solution

PidController（core/policy，实例类）：update
(setpoint,measurement)→控制量；抗积分
饱和；输出夹挤；Δt 等步契约。

## Testing Decisions

带负载一阶植物 PI 收敛 1e-3 优于 P-only；
Kd 抑振圣像；1000 拍饱和积分恒 0；
确定性；fail-fast。

## Out of Scope

不做增益自整定（Ziegler–Nichols 面）；
不做可变 Δt；不做前馈/串级（另立）；
不做传递函数域分析。

## Further Notes

与 EwmaEstimator 同域不同面；与
GradientBandit 同根不同面。开发勘误：
无负载植物 P-only 亦零差——负载使
余差显形。Wave 8 第二件。
