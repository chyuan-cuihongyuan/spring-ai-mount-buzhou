---
Type: task
Status: closed
---
## Question

BulkheadScalingAdvisor（core.concurrent）：窗口增量读舱拒绝 → 建议实例倍率
clamp(1 + 窗口拒绝/scaleUpThreshold, 1, maxMultiplier)；回零回落 1；
Advice 快照（agent/windowRejections/suggestedMultiplier）；scale-up/scale-down
事件计数（metrics + 类内观测）；参数校验。输入缝 = 舱拒绝计数表全量快照，
不开新缝。

## Resolution

done（2026-09-02）：impl-342；六用例绿。
