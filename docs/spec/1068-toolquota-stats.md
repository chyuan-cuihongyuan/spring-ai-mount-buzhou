# 1068 — 工具配额消耗读面

> 来源：J 会话第 68 轮 = effort #1068（[T1591](../../.wayfinder/tickets/T1591-toolquota-stats-shape.md) / [T1592](../../.wayfinder/tickets/T1592-toolquota-stats-verify.md) / impl 820）。借鉴：云厂商 per-API quota 消耗/拒绝对账（配额域自源思想的静态面延伸）。guard/hook 域首轴。

## Problem Statement

`ToolQuotaHook.beforeTool()`（spec 185 / T557：per-tool 会话配额，「放行即计、被拒不计」）的四条路径——放行、超限拒绝、未配置豁免、null 跳过——仅有 per-tool micrometer blocked 遥测：**配额消耗量（allowed）与未管辖豁免量无进程内直读面**。宿主无法回答「配额内消耗 vs 拒绝的比例、多大比例调用根本不在配额管辖内（未配置工具占多少流量）」——配额清单是否覆盖真实流量无从对账。

## 目标

- `ToolQuotaHook` 增量（guard/hook，静态面）：五 `AtomicLong`。
  - `calls`：beforeTool 入口计数（总桶）；
  - `allowed`（放行即计）/ `quotaBlocks`（超限拒绝）/ `unmanagedSkips`（null ctx/toolName 与未配置限制合并——「不在配额管辖内」口径）；
  - `excludedTokens`：坏状态值解析归零重计（NumberFormatException 静默修正显形）。
- 嵌套 `record ToolQuotaStats(long calls, long allowed, long quotaBlocks, long unmanagedSkips, long excludedTokens)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**calls = allowed + quotaBlocks + unmanagedSkips**（每入口恰落一桶；excludedTokens 为旁路修正量不占入口桶）。

## 兼容性

纯增量读面：beforeTool 返回语义、配额判定与「放行即计」语义逐位不变；micrometer 遥测原样保留（互补非替代）；静态面理由同 R46–R67 先例；无新配置项。

## Out of Scope

- 按 tool 名分桶（既有 micrometer tag 已覆盖）。
- 配额余量快照（会话态即真相源）。
