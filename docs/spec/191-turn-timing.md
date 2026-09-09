# Spec 191 — 轮次时延计时（effort #214）

> wayfinder map：`.wayfinder/maps/effort-214.md`（T563–T564）。补生产轮次的端到端
> 时延读数——eval run（spec 111）与工具（108）都有 timer，轮这一层没有。

## Problem Statement

「这个 agent 一轮要多久」是容量规划与降级决策的基础数：模型 timer 只覆盖
模型调用、工具 timer 只覆盖单工具——重试链/多工具并行/压缩折入的<b>端到端
轮时延</b>没有系统读数，宿主自己掐表散落各处。

## Solution

`TurnTimingHook`（core/hook）：

- **计时**：beforeTurn 记起点（per-session 内存表）；afterTurn 计端到端时长
  → 喂 `buzhou.turn.duration` timer（tag=agentName，有界）。
- **滚动窗读数**：per-session 最近 64 次样本 → `stats(sessionId)` =
  `TurnStats(count, avgMillis, maxMillis, lastMillis)`（容量规划直读）。
- 未完轮安全：beforeTurn 后会话再 beforeTurn（异常路径缺 afterTurn）——
  起点覆盖重计（不炸不错配）。LRU 1024 会话。

## User Stories

1. 作为运维，buzhou.turn.duration 的 p95 即「用户体感一-轮」基线——容量与
   SLA 有据。
2. 作为宿主，单会话 avg/max 读数支撑「长轮会话」识别（配检疫/排水）。
3. 作为策略，轮时延趋势劣化 = 提前扩容/降载信号。

## Implementation Decisions

- System.nanoTime 起点单调钟（跨 afterTurn 差值）；毫秒喂 timer。
- 计时永不影响轮本身（hook 面零阻塞——记表+计时两步）。

## Testing Decimals

- 计时正确（可控 sleep 下限断言）；滚动窗 64 截断与 avg/max；会话隔离；
  重入 beforeTurn 覆盖；timer 面由 metrics 实现验证（BuzhouMetrics mock 断言
  timer 被调即可）。

## Out of Scope

- 直方图桶配置；慢轮告警事件；跨会话聚合。

## Further Notes

- 时延三层：模型（既有 timer）/ 工具（108）/ 轮端到端（本轮）。
