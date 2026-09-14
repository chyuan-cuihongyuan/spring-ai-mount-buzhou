# 1098 — 冒烟补全轮

> 来源：J 会话第 98 轮 = effort #1098（[T1655](../../.wayfinder/tickets/T1655-smokeext2-shape.md) / [T1656](../../.wayfinder/tickets/T1656-smokeext2-verify.md) / impl 850）。R83 元验证轮的补全轮。

## Problem Statement

R83 统一反射冒烟清单未覆盖的 TodoTool（actionStats Map 形状）与 ToolSlowLog（实例 stats 形状）游离于冒烟之外。

## 目标

`ReadoutContractSmokeTest` 增两测试：todoActionStatsSmoke / toolSlowLogSmoke——按各自形状断言计数非负。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 两读面形状统一重构（另立）。
