# Spec 6041 — AIMD Window 加性增/乘性减窗口（effort #6041，T41）

> wayfinder map：`.wayfinder/maps/effort-6000.md`（T6281–T6282，impl 2241）。
> 借鉴：TCP 拥塞避免思想。源码本轮入档。

## Problem Statement

自适应限额的病：固定阈值限流无视拥塞信号、纯加性调节收敛
慢——**加性增/乘性减锯齿面**缺失。

## Solution

`AimdWindow`（core/ratelimit，源码已预载）：

- 成功 +1（钳 max）；失败 ×β 向上取整（钳 min）——乘性
  减快速退避、加性增缓慢探测（锯齿收敛经典）；
- 读数：current/minLimit/maxLimit/decreaseFactor；
- fail-fast：min>max、min<1、β∉(0,1)。

## User Stories

1. 作为限流作者，并发窗口随成败自适应——拥塞信号利用。
2. 作为审计作者，锯齿形态可复算——收敛可证。

## Testing Decisions

- 锯齿形态（18 增至 20→失败减半 10→再增 20→减半）逐值
  钉住；取整与 min 钳制语义；max 钳制；双实例确定性；
  参数校验 fail-fast。

## Out of Scope

- 不做超时/延迟信号（成败二值定构）；不做平滑增（每成功
  +1 定构）。

## Further Notes

- 与 GradientAdaptiveLimiter（concurrent）同族不同面：梯度
  延迟信号 vs 成败二值 AIMD 锯齿。
- 里程碑：T41/50（82%）。
