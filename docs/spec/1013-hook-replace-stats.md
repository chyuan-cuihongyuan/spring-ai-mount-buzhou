# 1013 — Hook Replace 载荷应用/丢弃计数读面

> 来源：J 会话第 14 轮 = effort #1013（[T1477](../../.wayfinder/tickets/T1477-replace-stats-shape.md) / [T1478](../../.wayfinder/tickets/T1478-replace-stats-verify.md) / impl 766）。与 R3 幽灵禁用同族：**静默蒸发显形**（Sentry discarded events / OPA decision log 的谱系思想——不可见即不可治）。

## Problem Statement

`HookChain.applyReplace` 的上下文类型分支与 `default` 臂存在静默丢弃：`Replace` 载荷与目标上下文不匹配时（如 beforeTurn 返回非 String 载荷、beforeModel 返回非 ChatClientRequest/Response 载荷）替换意图无声蒸发——钩子作者以为改写了请求/结果，实际什么都没发生，且无任何信号。

## 目标

- `HookChain.applyReplace` 改返回 boolean（是否真实应用）。
- HookChain 实例级 `replaceApplied` / `replaceDropped` 两 AtomicLong + `replaceAppliedCount()` / `replaceDroppedCount()` 读面。
- 分发行为逐位不变：丢弃仍静默不抛（本轮只显形不拦截——拦截属语义变化，out of scope）。

## 兼容性

纯增量读面：Replace 合法路径行为逐位不变；无新配置项。

## Out of Scope

- 丢弃升级 WARN/异常（语义变化另议；计数已给装配层自警依据）。
- 载荷类型校验前置（钩子作者侧契约工具，另立项）。
