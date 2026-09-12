# Wayfinder Map — Buzhou 时段路由窗口（effort #503，E 会话第 4 轮）

> E 会话第 4 轮。勘察：路由权重有平滑 WRR（199）/加权装配（339）/
> 热调（340 setWeight）——权重是**静态配置**（改 yml 发事件才变）；
> 「夜间切便宜模型、工作日白天切强模型」需人工值守。K8s CronJob+
> Argo Rollouts 流量窗口思想：时间窗驱动权重自动切换。

## Destination

`routing.RoutingScheduleAdjuster`（SmartLifecycle 轮询器——IdleCompaction
同法：单线程 scheduleAtFixedRate+异常隔离+stop 关停）：每 tick 用注入
Clock 取当前时刻 → 命中首窗（start≤t<end）取窗权（整表替换语义——
窗口 weights 即全量活跃权重，未知 bean 名经 setWeight 跳过不红——340
同口径）、无窗回落基础权重（BuzhouRoutingProperties.weights）；目标
快照 != 上次已应用 → 逐路 setWeight+WARN 留痕+applied/reverted 计数
（417 价目热载同款审计面）。
`BuzhouRoutingScheduleProperties`（buzhou.routing.schedule.{windows[],
check-interval 默认 30s ≥5s}；RoutingWindow(LocalTime start<end 同日窗,
weights 非空)——"25:00" 绑定失败启动红 fail-fast）。装配条件：windows
非空（Binder 预绑）——无 WeightedChatModel bean 时 bean 缺席诚实退化。

## Notes

- 号段：spec 503 / T757–T758 / impl-406。
- 借鉴源：K8s CronJob + Argo Rollouts canary schedule（时间窗流量切换）。
- 诚实边界：同日窗不跨午夜（start<end 严格——跨午夜窗用两窗拼）；
  轮询粒度非秒级精度（checkInterval 默认 30s）；多实例各自独立切换
  （无协调——时钟同源天然一致，415 亲和同思想）。

## Out of scope

- 星期/节假日历（同日窗先行）；跨午夜窗；秒级精度。

## Tickets

- [x] [T757 窗口解析与切换判定](../tickets/T757-routing-window-eval.md)
- [x] [T758 轮询器生命周期与装配](../tickets/T758-routing-schedule-assembly.md)
