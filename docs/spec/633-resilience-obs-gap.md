# 633 — resilience 观测/装配补验

> 来源：F 会话第 34 轮 = effort #600（spec 601/620 的补验双小件）/ [T916](../../.wayfinder/tickets/T916-resilience-obs-shape.md) / [T917](../../.wayfinder/tickets/T917-resilience-verify.md) / impl 486。

## 背景

panic 只有指标无编程面；time-window 无绑定用例——两处覆盖缺口。

## 目标

panicActivations() getter + time-window yml 绑定用例。

## 测试

Panic 计数断言 + 绑定用例；全模块零回归。

## 兼容性

getter 纯增量；测试纯增量。
