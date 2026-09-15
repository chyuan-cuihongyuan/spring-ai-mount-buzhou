# 1112 — readRange×Spotlight 组合测试轮

> 来源：J 会话第 112 轮 = effort #1112（[T1683](../../.wayfinder/tickets/T1683-readspot-shape.md) / [T1684](../../.wayfinder/tickets/T1684-readspot-verify.md) / impl 864）。纯测试轮第十七弹（跨模块组合回退为 core Spotlighting 直调形态）。

## Problem Statement

R62 回读与 Spotlight 包裹的生命周期组合（溢出→包裹→回读切片→幂等跳过）——R64 已验证 hook 层，core Spotlighting.wrap 与回读切片的直接组合无验证。

## 目标

新增 `ReadRangeSpotlightComboTest`（buzhou-spill）：溢出占位含标记段断言 + core Spotlighting.wrap 后回读切片幂等验证。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- guard SpotlightHook 跨模块联动（依赖方向受限）。
