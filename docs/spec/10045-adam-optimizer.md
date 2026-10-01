# Spec 10045 — AdamOptimizer 自适应矩估计（effort #10045，X46）

> wayfinder map：`.wayfinder/maps/effort-10000.md`（X10091–X10092，impl 2448）。
> 借鉴：Kingma–Ba 2015 思想——PyTorch/TensorFlow 同源

## Problem Statement

一阶随机优化的自适应步长面——动量+二阶矩归一的更新基座。

## Solution

AdamOptimizer（core/metrics）：minimize(Objective,double[],lr,β1,β2,ε,iters)——m=β1m+(1−β1)g、v=β2v+(1−β2)g²、m̂=m/(1−β1^t)、v̂=v/(1−β2^t)、w−=lr·m̂/(√v̂+ε)；纯函数无状态返回终值向量。

## Testing Decisions

对角二次碗 f=½Σa_i w_i²−Σb_i w_i 解析解 b/a 收敛 1e-3+首步手算锚（m̂=g0、v̂=g0²→步长≈lr·sign）+已到极小点梯度零不动面+确定性+fail-fast 五面。

## Out of Scope

不做 AdamW 解耦权重衰减（论文变体另立）；不做随机批量采样面（梯度全量由 Objective 供）；不做学习率调度器。

## Further Notes

深度学习优化器原语——Kingma–Ba 2015 ICLR；Wave 8 经典机器学习族第四件。
