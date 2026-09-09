---
Type: task
Status: closed
---
## Question

会话行为特征一次定义多处消费：四点自动累积 + LRU 有界查询面。

## Resolution

done（2026-08-30）：impl-293；SessionFeatureStore（原始计数存、比率派生零
陈旧 + LRU 1024）+ SessionFeaturesHook（beforeTurn/afterTool/onModelError）。
