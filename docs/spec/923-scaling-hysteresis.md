# 923 — 扩缩容建议缩容滞回

> 来源：I 会话第 24 轮 = effort #923（[T1297](../../.wayfinder/tickets/T1297-scaling-hysteresis-shape.md) / [T1298](../../.wayfinder/tickets/T1298-scaling-hysteresis-verify.md) / impl 676）。借鉴：Kubernetes HPA [stabilization window](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/#scaling-policies)——扩容即时、缩容经稳定窗防抖（不对称语义）。

## Problem Statement

`BulkheadScalingAdvisor` 拒绝回零即建议回落 1：单窗偶发拒绝（抖动）产生「扩→缩→扩」建议震荡，宿主跟随建议会抖动扩缩。HPA 的对策是缩容侧 stabilization window——连续多个窗口条件满足才缩。

## 目标

- `BulkheadScalingAdvisor` 新构造重载：`stabilizeWindows`（≥1；**默认 1 = 既有立即回落逐位不变**）；
- 回落 1 建议需连续 `stabilizeWindows` 个窗口拒绝为零；窗口期内保持上次非 1 建议；
- 扩容（非 1）建议路径即时性不变（不对称语义——扩容即 protective、缩容需确认）；
- 构造校验 stabilizeWindows ≥ 1（fail-fast）；
- 既有两参构造委托新重载（stabilizeWindows=1）——源/二进制兼容。

## 兼容性

opt-in：默认 1 时全部行为逐位不变。
