# Wayfinder Map — Buzhou 工具健康探测（effort #200，B 会话第 23 轮）

> B 会话第 23 轮。**号段声明：B 侧自本轮起固定 .wayfinder200+ 远端号段**
> （A 侧在 111-155+ 顺延，双方防撞——延续 #86 MAP 分工协议）。主题池
> 「工具健康探测」：工具可用性只有「调了才知道」——挂了的下游要等模型撞墙
> 回错。借鉴 Consul health check（周期探活 + 状态翻转事件）。

## Destination

ToolHealthProber（core/exec）：per-tool 探针 Callable（宿主注册轻量检查）；
probeOnce() 聚合（UP/DOWN + 连败计数 + 翻转通知）；可选自调度（interval）。
DOWN 计数 + 翻转监听（目录/熔断可消费）。

## Notes

- 号段：B=奇数 spec（本轮 165）。
- 探针归宿主定义（每工具知道怎么便宜地探——框架不知道）；探活异常=DOWN 不上抛。
- 与工具熔断（131）互补：熔断看真实调用结局（被动），探测主动问（提前发现）。

## Decisions so far

- 只在状态翻转时通知（不刷屏）；探活超时由探针自负。

## Not yet specified

- 目录联动（DOWN 工具自动摘牌）；autoconfig 定时装配。

## Out of scope

- 沿用 #7–#110；探针自动生成；分布式探测去重。

## Tickets

- [x] [T527 ToolHealthProber（探针注册/聚合/翻转通知）](tickets/T527-tool-probe.md)（impl-295）
- [x] [T528 探测回归（UP/DOWN/连败/恢复/翻转通知）](tickets/T528-tool-probe-tests.md)（impl-295）
