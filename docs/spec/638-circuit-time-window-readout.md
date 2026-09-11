# 638 — 熔断时间窗生效读面

> 来源：F 会话第 39 轮 = effort #600（spec 620 装配后的生效确认面）/ [T926](../../.wayfinder/tickets/T926-circuit-tw-readout.md) / [T927](../../.wayfinder/tickets/T927-circuit-tw-readout-verify.md) / impl 491。

## 背景

time-window 声明后生效值健康面不可读。

## 目标

`stats.details().circuitTimeWindowMs`（0=count 窗 / 正数=声明毫秒）。

## 测试

1 用例双态 + 全模块零回归。

## 兼容性

details 加键纯增量。
