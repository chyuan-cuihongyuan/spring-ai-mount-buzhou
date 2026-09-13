# 1015 — 超时覆盖命中读面

> 来源：J 会话第 16 轮 = effort #1015（[T1481](../../.wayfinder/tickets/T1481-timeout-override-stats-shape.md) / [T1482](../../.wayfinder/tickets/T1482-timeout-override-stats-verify.md) / impl 768）。与 R3 幽灵禁用同族：**配置静默失效显形**（feature-flag 评估计数思想——LaunchDarkly 评估统计）。

## Problem Statement

`ToolTimeoutOverrides`（spec 529）按 glob 首个命中返回 per-tool 覆盖毫秒，但零观测：配置键拼错工具名 = glob 永不命中 = 覆盖**静默失效**（想给慢工具放宽超时实际没生效）；「哪些覆盖配置在真被命中」不可见——配额/调优决策无据。

## 目标

- 新公共 record `ToolTimeoutOverrideStats(long lookups, long hits, long misses, Map<String, Long> hitsByPattern)`（core.exec，api 面）：守恒不变量 **hits + misses == lookups**；`hitsByPattern` 按配置模式分桶——**永不命中的模式即幽灵覆盖配置**（显形不拦截）。
- `ToolTimeoutOverrideStats` 由 `ToolTimeoutOverrides.stats()` 快照；`timeoutMillisFor` 返回值逐位不变（命中/未命中各计一次）。

## 兼容性

纯增量读面：解析/glob/返回值语义零变化；无新配置项。

## Out of Scope

- 幽灵模式升级启动失败/告警（语义变化另议——计数已给配置医生自警依据）。
- 命中时延分布（spec 108 timer 域）。
