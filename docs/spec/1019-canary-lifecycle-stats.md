# 1019 — 金丝雀生命周期计数读面

> 来源：J 会话第 20 轮 = effort #1019（[T1489](../../.wayfinder/tickets/T1489-canary-stats-shape.md) / [T1490](../../.wayfinder/tickets/T1490-canary-stats-verify.md) / impl 772）。借鉴：Thinkst [Canary](https://canary.tools/)（金丝雀被触发即入侵在场的铁证信号——零误报语义）。与 R14/R15 同族：安全判定静默显形。

## Problem Statement

CanaryGuardHook（播撒密语 → afterTool 检测泄漏 → 拦截 + 拒识记忆自硬化）三段全程零计数：播撒了几次、密语泄漏检出几次、变体自硬化拦截几次不可见——「泄漏/变体触发」是间接注入在场的铁证（Canary 零误报语义），无累计水位则告警与攻防复盘无据。

## 目标

- `CanaryGuardHook` 增量（buzhou-guard inject 包，实例级）：`planted` / `leaked` / `variantBlocked` 三 AtomicLong。
  - planted：beforeModel 实际注入密语时计（幂等重复注入不重复计）；
  - leaked：afterTool 密语泄漏拦截时计；
  - variantBlocked：变体（n-gram Jaccard 近邻）自硬化拦截时计。
- 嵌套 record `CanaryStats(long planted, long leaked, long variantBlocked)` + `stats()` 快照。

## 兼容性

纯增量读面：播撒/泄漏拦截/变体拦截/拒识记忆语义逐位不变；无新配置项。

## Out of Scope

- 按工具/会话分桶（基数纪律）。
- 计数升级独立告警通道（既有 guard.canary.* 事件已是事件面——本轮只补进程内水位）。
