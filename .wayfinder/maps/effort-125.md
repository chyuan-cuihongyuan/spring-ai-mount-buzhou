# Wayfinder Map — Buzhou 停滞巡检犬（effort #125，A 会话第 20 轮）

> A 侧票号 T501+ / spec 偶数段沿用。spec 152 fog「定期 stalled 查询 + 告警」
> 收口。借鉴 K8s liveness probe 周期自检 + Alertmanager 分层（发现/去重分工）。

## Destination

TurnStallWatchdog：低频单线程轮询在飞全集——quiet 超阈值者每轮交付 listener
（只发现不处置）；手动 inspectOnce 恒可用。

## Notes

- 重复告警语义诚实：每轮都报（停滞是持续状态），去重/静默归接收端；
  空表也通知（「跑过无事」是事实）；单轮异常不杀调度线程。
- TurnHeartbeat 补 registered() 全集视图（注册制事实表——巡检犬无需宿主供名单）。

## Decisions so far

- [TurnStallWatchdog](../tickets/T515-stall-watchdog.md) — SmartLifecycle 同骨架 +
  listener + WARN 日志。

## Not yet specified

- autoconfig yml 键（buzhou.runaway.stall-watchdog.*）；停滞事件入观测导出。

## Out of scope

- 自动处置（软取消/告警动作——listener 宿主定夺）。

## Tickets

- [x] [T515 停滞巡检犬](../tickets/T515-stall-watchdog.md)（impl-292）
- [x] [T516 收口提交](../tickets/T516-stall-watchdog-close.md)（impl-292）
