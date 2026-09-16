# Spec 2036 — 双阈值迟滞水位门（effort #2036，R37）

> wayfinder map：`.wayfinder/maps/effort-2000.md`（T3173–T3174，impl 1587）。
> 借鉴：Netty write buffer watermark——高停低续迟滞防抖。

## Problem Statement

单阈值背压门的已知病是抖动：积压在阈值附近波动时 writable 反复翻转
（背压开开关关——生产者被高频启停）。上下文水位（181/526）是单阈值
告警面，非生产者背压门。

## Solution

`HysteresisWatermark`（core/backpressure，纯记账无副作用——停/续动作
归调用方）：

- `onWrite(bytes)`：pending 累加；**超 HIGH 停写**（writable=false）；
- `onDrain(bytes)`：pending 递减钳 0；**泄到 ≤ LOW 才恢复**
  （writable=true）——HIGH 与 LOW 之间是**保持区**（状态延续不翻转，
  迟滞核心）；
- `toggleCount` 翻转计数（抖动显形——频繁翻转 = 两阈值过近诊断）；
- 读数：isWritable / pendingBytes；契约：low ≥ 0、high > low（重合/
  倒置 fail-fast——退化单阈值抖动门不合法）、bytes ≥ 0 fail-fast。

## User Stories

1. 作为生产者背压作者，水位附近波动不再翻转——背压状态稳定。
2. 作为调参者，toggleCount 高 = 两阈值过近——拉大间隙即可。

## Testing Decisions

- 恰 HIGH 不停/超 1 停；泄到 110 保持停/钳 0 恢复；保持区全程恰两翻
  转（无抖动）；恰 LOW 恢复（≤ 语义）；超泄钳 0；累计追踪；畸形五
  型 fail-fast。

## Out of Scope

- 不做多通道共享水位（单账本口径）；不接 exec/写管线（接线归后续轮）。

## Further Notes

- 与 BurstCreditAccount/SpawnGate 同域互补：速率账户/准入门/水位门
  三形态。
