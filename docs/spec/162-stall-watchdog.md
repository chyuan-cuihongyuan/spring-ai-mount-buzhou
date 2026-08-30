# Spec 162 — 停滞巡检犬（effort #125）

> wayfinder map：`.wayfinder125/MAP.md`（T515–T516）。spec 152 fog「定期
> stalled 查询 + 告警」收口。借鉴：K8s liveness probe 周期自检。

## Problem Statement

心跳表有了、stalled 查询有了，但没人定期问——停滞发现依赖宿主自觉轮询，
卡死轮次可能永远没人看。

## Solution

`runaway/TurnStallWatchdog`（SmartLifecycle，ArchivePurgeJob 同骨架）：低频
单线程轮询 `TurnHeartbeat.registered()` 全集（注册制事实表——无需宿主供
名单）；quiet 超阈值者按停滞时长降序交付 listener（告警/事件由宿主定夺，
本犬只发现不处置）。**重复告警语义诚实**：每轮都报——停滞是持续状态，
「还在停」每轮都是事实；去重/静默窗口归告警接收端。空表也通知（「跑过无事」
是事实）；单轮异常只记 ERROR 不杀调度线程。

## User Stories

1. 作为 SRE，挂一个 listener 就有周期性停滞告警，所以卡死轮次最迟一个巡检
   周期内上墙。
2. 作为宿主开发者，默认不排程 + inspectOnce 手动面，所以测试确定性驱动、
   部署默认零变化。

## Testing Decisions

- 红队：单轮全集自取 + quiet 降序 + 清空后空表通知；40ms 周期重复告警
  （每轮含 stuck）+ stop 后不再触发；默认不排程 + 参数 fail-fast + null
  心跳宽进。心跳族三测回归。

## Out of Scope

- autoconfig yml；自动处置；停滞事件入观测导出。

## Further Notes

- 心跳三件套完整：TurnHeartbeat（表）/ TurnHeartbeatHook（打点）/
  TurnStallWatchdog（发现）。
