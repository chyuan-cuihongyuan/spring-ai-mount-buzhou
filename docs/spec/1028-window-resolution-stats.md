# 1028 — 模型窗口解析分布读面

> 来源：J 会话第 28 轮 = effort #1028（[T1507](../../.wayfinder/tickets/T1507-window-resolution-stats-shape.md) / [T1508](../../.wayfinder/tickets/T1508-window-resolution-stats-verify.md) / impl 781）。与 R16 同谱系（配置命中显形）；借鉴：LLM 提供商模型目录覆盖思想（解析路径三路分布直读）。

## Problem Statement

TableContextWindowResolver（spec 707）resolveWindow 三路解析（yml 覆盖 → 内置表前缀 → 未知回退 32K）零计数：①yml 覆盖键拼错模型名 = 永不命中（幽灵覆盖——与 R16 超时覆盖同族）；②未知模型回退 32K 的规模不可见（告警每模型只一条）；③实际解析过的模型窗分布不可查。

## 目标

- `TableContextWindowResolver` 增量（core/token，实例级）：`overrideHits` / `builtInHits` / `fallbackHits` 三 AtomicLong + `resolvedWindows`（已解析模型 → 窗值快照，有界——模型名源自应用配置）。
- 嵌套 record `WindowResolutionStats(long overrideHits, long builtInHits, long fallbackHits, Map<String, Integer> resolvedWindows)` + `stats()` 快照。
- 三路守恒不变量：**三计数和 == resolveWindow 调用总数**（含 null 模型）。
- 解析返回值逐位不变（覆盖优先 → 内置前缀 → 32768 回退 + 每模型一次 WARN 原语义）。

## 兼容性

纯增量读面；无新配置项。

## Out of Scope

- 内置表内容扩充（模型目录演进另行维护）。
- resolvedWindows 持久化（进程内快照）。
