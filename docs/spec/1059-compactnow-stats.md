# 1059 — compact_now 手动压缩判定读面

> 来源：J 会话第 59 轮 = effort #1059（[T1573](../../.wayfinder/tickets/T1573-compactnow-stats-shape.md) / [T1574](../../.wayfinder/tickets/T1574-compactnow-stats-verify.md) / impl 811）。借鉴：Anthropic /compact 采用率（模型主动维护上下文的行为分布是提示词引导有效性的直接信号）。与 R53 evict_handle 同谱系分轴（那轴是句柄逐出，本轴是历史压缩）。

## Problem Statement

`CompactNowTool`（模型主动触发上下文压缩）的四条路径——会话未绑定、无需压缩（待折入为空）、压缩失败、压缩成功（含新折入条数）——当前只返回字符串：**模型主动压缩行为的采用率与成败分布不可见**。宿主无法回答「模型是否学会用 compact_now 主动治理上下文、失败集中在哪个环节」；提示词里对压缩工具的引导是否生效完全无信号。

## 目标

- `CompactNowTool` 增量（memory/tool，静态面）：五 `AtomicLong`。
  - `calls`：call 入口计数（总桶）；`successes`（压缩完成）/ `skippeds`（无需压缩）/ `failures`（压缩失败）/ `unboundRejects`（会话未绑定）四个结局桶。
- 嵌套 `record CompactNowStats(long calls, long successes, long skippeds, long failures, long unboundRejects)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**calls = successes + skippeds + failures + unboundRejects**（每入口恰落一桶）。

## 兼容性

纯增量读面：call() 返回语义、压缩触发与会话绑定解析逐位不变；静态面理由同 R46–R58 先例；无新配置项。

## Out of Scope

- foldedMessages 条数分布（结果面在 MicroCompactionResult，调用方可见）。
- 按 sessionId 分桶（会话标识敏感面——红线纪律）。
