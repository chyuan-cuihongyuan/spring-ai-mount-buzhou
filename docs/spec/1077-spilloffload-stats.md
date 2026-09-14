# 1077 — Spill 溢出 hook 判定读面

> 来源：J 会话第 77 轮 = effort #1077（[T1609](../../.wayfinder/tickets/T1609-spilloffload-stats-shape.md) / [T1610](../../.wayfinder/tickets/T1610-spilloffload-stats-verify.md) / impl 829）。借鉴：logrotate 轮转率（溢出触发率是管线容量规划的第一信号）。spill 域主 hook 轴（R53/R54/R62 的上游）。

## Problem Statement

`SpillOffloadHook.afterTool()`（超阈值工具输出溢出落盘主 hook）的判定路径——error 跳过、durable 覆盖跳过、阈值内全量内联、溢出替换、REFRAIN 降级——全部零计数：**溢出触发率与降级动作分布不可见**。宿主无法回答「多大比例工具输出在溢出、durable 声明拦下多少、REFRAIN 降级是否高发」；阈值调优与容量规划无数据支撑。

## 目标

- `SpillOffloadHook` 增量（spill，静态面）：六 `AtomicLong`。
  - `invocations`：afterTool 入口（总桶）；
  - `durableSkips`（durable 覆盖永不溢出）/ `errorSkips`（error/null 结果）/ `cleanInline`（阈值内全量内联）/ `offloaded`（溢出替换含数组逐项）/ `refrains`（OnFail.REFRAIN 降级）。
- 嵌套 `record SpillOffloadStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**invocations = durableSkips + errorSkips + cleanInline + offloaded + refrains**（每入口恰落一桶）。

## 兼容性

纯增量读面：afterTool 返回与替换语义、阈值判定与数组逐项溢出逐位不变；静态面理由同 R46–R76 先例；无新配置项。

## Out of Scope

- 按 toolName 分桶（SpillThresholds 配置面）。
- 溢出字节数（store 层既有口径）。
