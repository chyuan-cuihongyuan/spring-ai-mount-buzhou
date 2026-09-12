# 725 — 路由慢启动 yml 装配

> 来源：G 会话第 26 轮 = effort #725（D 会话装配轮模式）/ [T1001](../../.wayfinder/tickets/T1001-slow-start-yml-shape.md) / [T1002](../../.wayfinder/tickets/T1002-slow-start-yml-verify.md) / impl 528。

## 背景

RoutingSlowStart（spec 702）只有编程面——热重载权重上调的爬坡对 yml 声明式部署不可用。

## 目标

- `BuzhouRoutingProperties` 增 `slowStart` 字段（`buzhou.routing.slow-start`；null=关；负值 fail-fast）。
- autoconfig 增 `buzhouRoutingSlowStart` bean（routingConfigured 且声明才装配；AutoCloseable→close 关停调度器）。
- 热重载 bean 注入 `ObjectProvider<RoutingSlowStart>`——在场走 3 参构造（上调爬坡），缺席走 2 参（零变化）。

## 测试

properties 三态绑定；装配接线直调（声明 → ramp 生效、缺省 → 瞬时）；既有用例零回归。

## 兼容性

缺省逐字节不变。
