# Spec 308 — deadline 跨工具传播（effort #308）

> wayfinder map：`.wayfinder/maps/effort-308.md`（T607–T608）。借鉴：gRPC deadline
> 逐跳传播（deadline 属于调用链，每一跳可见剩余）。

## Problem Statement

Turn 硬 Deadline 只在管理器侧强制（超时取消），工具侧不可见：自限型工具
（搜索翻页、批量拉取、递归展开）不能按剩余预算自我收敛——做了注定被取消
的工作。

## Solution

`HarnessToolCallingManager` 把当前 TurnDeadline（动态视图）放进 ToolContext：

- 键 `buzhou.turnDeadline`；静态助手 `turnDeadlineOf(toolContext)` 取用
  （缺省/非 TurnDeadline → `TurnDeadline.none()` 哨兵）。
- remainingMillis 实时递减（工具可轮询自限）；无 Deadline（未配置）时
  哨兵 isNone——工具自由放行，既有无限等待语义不变。

## User Stories

1. 作为工具作者，读剩余预算决定翻多少页——大概率在预算内完成而非被硬取消。
2. 作为运维，自限工具与硬取消双层兜底——预算收敛 + 强制上限互不替代。

## Implementation Decisions

- 透传 TurnDeadline 对象本身（免快照失真——工具读的是实时剩余）。

## Testing Decisions

- `DeadlinePropagationTest`：deadline 设置时工具读到正剩余且随批内推进
  递减；无 deadline 时 isNone 放行；助手对缺键/异型值返回哨兵。

## Out of Scope

- 跨进程传播（MCP 出站预算头）。

## Further Notes

- 执行脊柱 deadline 面：硬取消（13 §core-2 既有）+ **工具可见（本轮）**；
  出站传播归 MCP 族。
