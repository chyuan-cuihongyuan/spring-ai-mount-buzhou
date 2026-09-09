---
Type: task
Status: closed
---
## Question

停滞巡检犬：全集轮询 + listener 交付 + 每轮重复告警诚实语义。

## Resolution

done（2026-08-30）：impl-292；`runaway/TurnStallWatchdog`（inspectOnce/start/
stop/listener + heartbeat 视图）+ TurnHeartbeat.registered() + 红队 3 例。
