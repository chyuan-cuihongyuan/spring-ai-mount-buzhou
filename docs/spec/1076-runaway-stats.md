# 1076 — Runaway 预算 hook 判定读面

> 来源：J 会话第 76 轮 = effort #1076（T1607–T1608 票对 / impl 828）。借鉴：上游闸门空结果率思想同族（runaway 终止频次是预算配置合理性的直接信号）。core/runaway 域第二轴（R33 重复检测姊妹）。

## Problem Statement

`RunawayHook.beforeModel()`（预算 runaway 主判定：wall-clock/轮步数/会话步数三硬顶）分支零计数——**runaway 终止频次与放行量不可见**：预算配置过紧（频繁硬顶阻断）或过松（从不触发）均无量化信号。

## 目标

- `RunawayHook` 增量（core/runaway，静态面）：四 `AtomicLong`。
  - `invocations`：beforeModel 入口；`blocked`（三硬顶任一触发——wall-clock/轮步数/会话步数合桶）；
  - `allowed`（正常放行）/ `disabledSkips`（机制关闭跳过）。
- 嵌套 `record RunawayStats(...)` + `stats()` + `resetForTest()`。
- 守恒恒等式：**invocations = blocked + allowed + disabledSkips**（每入口恰落一桶）。

## 兼容性

纯增量读面：beforeModel 返回语义、三硬顶判定与事件发射逐位不变；静态面理由同 R46–R75 先例；无新配置项。

## Out of Scope

- 按硬顶维度分桶（reason 已在 hard-stop 事件 payload）。
- 软阈值事件统计（soft-threshold 每轮首次语义既有）。
