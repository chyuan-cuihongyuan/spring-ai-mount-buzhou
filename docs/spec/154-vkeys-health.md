# Spec 154 — 虚拟 key 健康面（effort #122）

> wayfinder map：`.wayfinder/maps/effort-122.md`（T507–T508）。spec 148 fog「key 配额
> 可观测面」收口。

## Problem Statement

key 级预算闸落地后，配额状态只能编程查询（topUsage/isExhausted）——运维在
健康端点看不到「哪个 key 见顶、还剩多少」。

## Solution

`health/VirtualKeysHealth`：恒 UP（观测面——耗尽由预算闸拦截，健康面只报
事实）；details = distinctKeys + exhaustedKeys（全量计数）+ topUsage 前 8 行
（key/used/limit/exhausted 行内标记——健康详情有界纪律）。autoconfig 以
`@ConditionalOnBean(VirtualKeys)` 装配：宿主声明 VirtualKeys bean 时段自动
出现，编程面默认无（零装配零开销）。keys 缺席 = UNKNOWN + disabled。

## User Stories

1. 作为运维，我在 /actuator/buzhou 一屏看到哪个 key 见顶/将见顶，所以窗口
   reset 或提额的动作有准星。
2. 作为宿主开发者，不声明 VirtualKeys bean 就没有这个段，所以默认部署零变化。

## Testing Decisions

- 红队：恒 UP + top-8 有界（12 key 只显 8，distinct 全量）+ exhausted 标记与
  全量计数一致；null = UNKNOWN disabled。启动校验回归。

## Out of Scope

- reset 操作端点；成本列；DOWN 判定。

## Further Notes

- 与 spec 148 组合成 key 配额的闸位+观测闭环。
