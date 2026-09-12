---
Type: task
Status: closed
---
## Question

`CostSpikeDetector`：SpendRateRing 基线 z-score（均值/标差+minSamples+
绝对地板+零方差处理）→ SpikeEvent listener+计数；cooldown 防抖。

## Resolution

done（2026-09-12）：impl-411；触发/不足/地板/冷却用例绿（Clock 注入）。
