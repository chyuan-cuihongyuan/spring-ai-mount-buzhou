# Spec 1867 — 关键路径长度（effort #1867，R68）

> wayfinder map：`.wayfinder/maps/effort-1800.md`（T2935–T2936，impl 1468）。借鉴：
> 项目管理 CPM（关键路径法）——并行 DAG 最长加权路径=总时长下界：
> 缩非关键任务白花力气，关键任务延一秒总长延一秒。

## Problem Statement

并行编排（多步工具链/流水线 turn）的总时长靠直觉估：优化了非关键任务
 总时长不动（白花力气）、关键任务悄悄变长没人知道——「该优化哪个」的
 数学答案（最长链）没有计算面。

## Solution

`CriticalPathLength`（core/exec，静态纯函数）：

- `Task(id, durationMillis)` / `Dependency(from, to)` 契约构造（零时长
  合法——纯依赖占位；重复任务 fail-fast）；
- `longestPath(tasks, dependencies)`：Kahn 拓扑消元 + EF[v] = dur[v] +
  max(EF[pred]) DP → `Result(criticalPathMillis, terminalTask)`；
- 环 fail-fast（消元未尽带剩余数）；边端点不在任务集 fail-fast。

## User Stories

1. 作为编排作者，a(10)→b(100) 与 a→c(5)→d(10) 两分支——关键 110 走
   a+b：优化 c/d 一毫秒不值，优化 b 每毫秒都算数。
2. 作为延迟治理者，关键路径变化即总时长风险面变化——监控有锚点。
3. 作为框架宿主，任务与依赖口径自声明，纯计算不排程。

## Implementation Decisions

- 纯计算；前驱查取 O(V·E)（小图诚实边界——任务级 DAG 规模；大图换双
  邻接表归未来静脉）。

## Testing Decisions

- 串行链求和；分支取长+终点任务；单任务/空/非连通；环与端点缺失
  fail-fast；畸形三型 fail-fast。

## Out of Scope

- 不输出完整路径序列（只长度+终点）；不做松弛量（slack 归未来静脉）。

## Further Notes

- 与 FanoutPacingPlan/DrainForecast 互补：起发节奏/停机预测之外的
  总时长下界计算。
