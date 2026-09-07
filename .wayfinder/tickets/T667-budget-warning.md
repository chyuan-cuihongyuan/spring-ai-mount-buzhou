---
Type: task
Status: closed
---
## Question

软预警逻辑：三维判定（会话总量/成本/虚拟 key）整数化百分比、一次
一发（per session×dimension / per key×dimension）、已耗尽不预警。

## Resolution

done（2026-09-04）：impl-361；TokenBudgetHook afterModel 记账点后
warnIfCrossing；warned 集 1024 上限诚实降级。五用例（交叉一次/未达
不发/-1 关闭/成本维度/key 维度）绿。
