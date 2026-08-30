---
Type: task
Status: closed
---
## Question

心跳钩子：轮次起止自动注册清除 + 四点打点 + 共享视图。

## Resolution

done（2026-08-30）：impl-288；`runaway/TurnHeartbeatHook`（order 50 早段 +
六挂点 CONTINUE）+ e2e 2 例（悬挂模型下 inFlight/beat/clear 生命周期）。
