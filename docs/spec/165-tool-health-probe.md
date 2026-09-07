# Spec 165 — 工具健康探测（effort #111）

> wayfinder map：`.wayfinder/maps/effort-111.md`（T527–T528）。借鉴：Consul health
> check——周期探活 + 状态翻转事件（工具可用性从「调了才知道」变「提前知道」）。

## Problem Statement

工具挂了（下游宕机/凭证过期/网络分区）只有模型真实调用撞墙回错才暴露——
一轮预算被浪费、用户体验一次失败。Consul 对服务的解法是主动健康检查：
周期探活，状态翻转（UP↔DOWN）时广播。

## Solution

`ToolHealthProber`（core/exec）：

- **注册**：`register(toolName, Callable<Boolean> probe)`——探针归宿主
  （每工具知道怎么便宜地探；框架不知道）；重复注册覆盖。
- **探测**：`probeOnce()`——逐工具执行探针（异常/超时自负 → false），聚合
  `Map<String, ToolStatus>`（UP/DOWN + consecutiveDown）；DOWN 计数
  `buzhou.tool-probe.down`。
- **翻转通知**：`onChange(BiConsumer<String,ToolStatus>)`——仅在状态翻转时
  回调（不刷屏）；消费方（目录摘牌/熔断联动）按需接。
- **自调度（可选）**：`start(interval, executor)` 周期 probeOnce；`stop()`。

## User Stories

1. 作为运维，工具 DOWN 在模型撞墙前就被翻转事件暴露——恢复也是事件（双向）。
2. 作为策略，consecutiveDown 是「持续不可用」侧写——可接自动摘牌。
3. 作为宿主，探针我自己写（每工具不一样），框架管聚合与通知。

## Implementation Decisions

- 探测串行（工具数十量级 + 探针轻量；并行化留档）。
- 状态 map 稳定序；probeOnce 幂等可重入。

## Testing Decisions

- 探针 true → UP；抛异常/false → DOWN 连败累计；恢复 → UP 连败清零；
  翻转监听只在变化时收；覆盖注册；无探针空聚合。

## Out of Scope

- 目录自动摘牌联动；autoconfig；探针并行化。

## Further Notes

- 工具可用性三面：熔断（131，被动真实结局）→ 探测（本轮，主动提前）→ 摘牌（留档）。
