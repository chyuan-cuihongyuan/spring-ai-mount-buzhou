# Spec 503 — 时段路由窗口（effort #503）

> wayfinder map：`.wayfinder/maps/effort-503.md`（T757–T758）。E 会话第 4 轮。

## Problem Statement

路由权重（199/339/340）是静态配置——「夜间切便宜模型、白天切强模型」
需人工值守改 yml 发事件。时间窗驱动的流量切换（K8s CronJob/Argo
Rollouts schedule 思想）空白。

## Solution

`routing.RoutingScheduleAdjuster` + `BuzhouRoutingScheduleProperties`：

- **Properties**：`buzhou.routing.schedule.{windows[], check-interval}`。
  RoutingWindow = (LocalTime start, LocalTime end, Map<String,Integer>
  weights)——同日窗 start<end 严格（跨午夜不预设，两窗拼）、weights
  非空；"25:00" 绑定失败启动红。checkInterval 默认 30s、下限 5s。
- **Adjuster**（SmartLifecycle——IdleCompaction 同法）：每 tick（注入
  Clock）当前时刻命中首窗（start≤t<end，序优先）→ 活跃权重 = 窗
  weights **整表替换**（未知 bean 名 setWeight 跳过不红——340 同口径）；
  无命中窗 → 基础权重（BuzhouRoutingProperties.weights）。目标快照
  != 上次已应用才逐路 setWeight + WARN 留痕 + `buzhou.routing.schedule.
  {applied,reverted}` 计数——同窗内 tick 幂等零动作。
- **装配**：windows 非空（Binder 预绑）才出 bean；无 WeightedChatModel
  时 adjuster bean 缺席（ObjectProvider 诚实退化）。

## User Stories

1. 作为成本宿主，我想声明「22:00-08:00 用便宜模型池」，so 夜间流量
   自动切到低成本模型白天自动回切（零人工值守）。
2. 作为运维，我想每次权重切换有日志留痕与计数，so 什么时候切的、切
   成什么可审计（417 同款审计面）。

## Implementation Decisions

- 整表替换语义（窗口 weights 即全量活跃权重）——部分覆盖易留"幽灵
  权重"，整表可预测可审计。
- 多实例各自独立切换：无协调、时钟同源天然一致（415 亲和同思想）。
- tick 异常隔离防调度线程死亡（housekeeper 同法）。

## Testing Decisions

- Clock 注入：窗内 tick 应用窗权（routes() 断言）、窗外 tick 回落
  基础、同目标重复 tick 零动作（幂等）。
- 多窗序优先（首命中）；未知 bean 名跳过不炸。
- 生命周期 start/stop；yml windows 装配/缺席。

## Out of Scope

- 星期历、跨午夜窗、秒级精度、跨实例协调。

## Further Notes

- 新公共类型 `RoutingScheduleAdjuster`、`BuzhouRoutingScheduleProperties`
  随轮 regenerate 快照 + api-surface.md 加行。
