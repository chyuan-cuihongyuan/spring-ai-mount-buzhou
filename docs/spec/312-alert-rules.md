# Spec 312 — 健康告警规则（effort #312）

> wayfinder map：`.wayfinder/maps/effort-312.md`（T615–T616）。借鉴：Grafana ruler
> （规则即声明、数据源即健康面、通道即回调）。

## Problem Statement

BuzhouHealth 健康面（机制 DOWN 严格口径）只有 actuator 轮询可见——机制
失能（存储不可写/审计链断裂）没有进程内告警通道，运维要自己盯端点。

## Solution

`AlertRuleEngine`（core.health，SmartLifecycle）+ `BuzhouAlertProperties`：

- yml：`buzhou.alert.rules[*].{name, mechanism, for}`（for = 持续窗防抖，
  默认 0 立即）+ `buzhou.alert.interval`（默认 30s）。
- 周期评估：机制健康 DOWN 持续满 for 窗 → FIRING 通知；恢复 UP →
  RECOVERED 通知（双向）。通知 = 宿主回调 `Consumer<AlertFiring>` +
  `buzhou.alert.{fired,recovered}` 计数 + WARN 日志。
- 规则引用不存在的机制 → 启动 fail-fast；UNKNOWN ≠ DOWN（未启用机制
  不告警——BuzhouHealth 契约）；无规则 = 不装配（零变化）。

## User Stories

1. 作为运维，yml 一行声明「memory DOWN 持续 2 分钟就叫我」——分页通道
   自接回调。
2. 作为运维，flap 抖动被 for 窗吸收——不半夜被叫醒。

## Implementation Decisions

- 数据源 = BuzhouHealth bean 集（唯一可读有界源——metrics 只写不可读，
  勘察结论入档）。
- 引擎在 start() 时解析健康 bean 集（SmartLifecycle 晚于全部 bean 创建，
  规避同配置类条件可见性坑）。

## Testing Decisions

- `AlertRuleEngineTest`（stub 健康面 + 手动 tick 时钟）：DOWN 满窗触发 +
  UP 恢复双向 / for 窗内恢复不触发 / UNKNOWN 不触发 / 引用缺失机制
  fail-fast。
- 装配：yml 规则绑定 + 无规则无 bean。

## Out of Scope

- 指标阈值规则（需可读注册表——观测族后续）；告警路由/静默。

## Further Notes

- 运维族：健康面（13 §T66）/ 维护门（205）/ **告警规则（本轮）**。
