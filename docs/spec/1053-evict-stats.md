# 1053 — evict_handle 逐出判定读面

> 来源：J 会话第 53 轮 = effort #1053（[T1561](../../.wayfinder/tickets/T1561-evict-stats-shape.md) / [T1562](../../.wayfinder/tickets/T1562-evict-stats-verify.md) / impl 805）。借鉴：Anthropic tool_result 清除（模型主动压缩行为的采用率是其可学习性的第一信号）。J 会话首个 spill 域轮。

## Problem Statement

`EvictHandleTool.call()`（wayfinder2 impl-16 / T44）的全部路径——成功逐出、非法 URI 拒绝、解析错误兜底——当前只返回字符串：**模型主动逐出行为的成败分布不可见**。宿主无法回答"模型是否学会用 evict_handle 腾上下文（采用率）、失败集中在路径格式错误还是入参结构错误"；逐出教学（提示词引导）是否生效完全无信号。

## 目标

- `EvictHandleTool` 增量（spill，静态面）：四 `AtomicLong`。
  - `attempts`：call 入口计数（总桶）；`evictions`：成功 markEvicted 计数；
  - `badPathRejects`（非 spill:// URI）/ `parseRejects`（解析错误 catch 兜底）两个拒绝桶。
- 嵌套 `record EvictStats(long attempts, long evictions, long badPathRejects, long parseRejects)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**attempts = evictions + badPathRejects + parseRejects**（每入口恰落一桶）。

## 兼容性

纯增量读面：call() 返回语义、会话隔离与墓碑收缩联动逐位不变；静态面理由同 R46–R52 先例；无新配置项。

## Out of Scope

- 按 path 分桶（敏感面——红线纪律）。
- 墓碑收缩效果计量（视图生成侧，另轴）。
