# 1067 — 内容安全词表双缝判定读面

> 来源：J 会话第 67 轮 = effort #1067（[T1589](../../.wayfinder/tickets/T1589-moderation-stats-shape.md) / [T1590](../../.wayfinder/tickets/T1590-moderation-stats-verify.md) / impl 819）。借鉴：OpenAI moderation 双缝覆盖对账（输入/输出两缝的动作分布是词表防线有效性的直接读面）。guard/moderation 域首轴。

## Problem Statement

`ContentModerationHook`（spec 515 / T781 内容安全词表：beforeTurn 输入缝 + afterTool 工具输出缝，BLOCK/MASK 双动作）的双缝入口仅有 micrometer hits 计数：**无命中跳过、null 跳过、BLOCK vs MASK 动作分布无进程内直读面**。宿主无法回答「词表防线每天拦了多少、MASK 与 BLOCK 的比例、双缝里哪条缝在命中」的本地快照对账（micrometer 需后端聚合，测试与无 meter 部署不可直读）。

## 目标

- `ContentModerationHook` 增量（guard/moderation，静态面）：五 `AtomicLong`。
  - `invocations`：双缝入口合计（总桶）；
  - `blocked`（BLOCK 拦截）/ `masked`（MASK 替换）两个动作桶；
  - `cleanSkips`（无命中）/ `nullSkips`（null/空输入与 error/null 结果）两个跳过桶。
- 嵌套 `record ModerationStats(long invocations, long blocked, long masked, long cleanSkips, long nullSkips)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**invocations = blocked + masked + cleanSkips + nullSkips**（每入口恰落一桶，双缝共用桶集）。

## 兼容性

纯增量读面：双缝 HookResult、MASK 替换与 BLOCK 告示语义逐位不变；micrometer 遥测原样保留（互补非替代——R57 先例）；无新配置项。

## Out of Scope

- 按命中词分桶（词表内容敏感面——红线纪律）。
- 按缝细分（缝信息已在既有 micrometer tag）。
