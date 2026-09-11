---
Type: task
Status: closed
---
## Question

`LatencySloMonitor`（BuzhouHook）：TurnTimingHook 计时同法，坏事件
=elapsed>threshold → ErrorBudget.record(agent, ok)；budget() 观测面。

## Resolution

done（2026-09-12）：impl-412；慢模型 breach/快模型不 breach 用例绿。
