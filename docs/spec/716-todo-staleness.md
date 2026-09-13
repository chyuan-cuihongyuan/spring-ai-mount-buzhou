# 716 — Todo 陈旧度审计读数

> 来源：G 会话第 17 轮 = effort #716（todo 工具族治理面）/ [T1032](../../.wayfinder/tickets/T1032-todo-staleness.md) / [T1033](../../.wayfinder/tickets/T1033-todo-staleness-verify.md) / impl 616。

## Problem

todo 工具的任务清单由 agent 自管——`in_progress` 状态挂十几轮不收敛、`pending` 永远排不上是 agent 任务失控的第一信号，但没有读数面：宿主/运维看不到「这个会话的任务清单已经烂掉了」，agent 自己也不会主动看。OpenHands/Claude Code 等前沿 agent 实践里 todo 纪律面板是标配。

## Solution

- `TodoStalenessAudit`（buzhou-tools/todo，纯函数静态原语）：
  - `analyze(items, currentTurn, staleAfterTurns)` → `Report(rows, byStatus, stale, oldestOpenAge)`；
  - 每项 `Row(id, status, age)`——age = currentTurn − createdTurn（完成项也可读 age，供「多快收尾」分析）；
  - 滞留判定：status ≠ completed 且 age > staleAfterTurns；rows 按 age 降序（最老滞留排首）；
  - `promptHint()`：一行人话（「2 项滞留 in_progress（最老 12 turn）：#t3, #t7」）——供提示词注入面/健康面板拼装；
  - `staleAfterTurns ≤ 0` = 不判滞留（只报统计——零口径侵入）。

## User Stories

1. 会话健康：面板接 analyze——「这个会话滞留 5 项、最老 12 turn」→ 触发人工/自动干预。
2. 提示词注入：promptHint 拼进下一轮 system——agent 自察觉收敛任务（读数面不强制行为）。

## Implementation Decisions

- 轮次口径（非墙钟）：TodoItem 只有 createdTurn——跨会话墙钟语义归 store 层（诚实边界）。
- 纯函数：不写状态、不自动 complete/cancel（动作归 agent/宿主）。

## Testing Decisions

- 混合状态+年龄构造：滞留集合精确、rows 降序、byStatus 计数、oldestOpenAge。
- completed 项即便 age 大也不入滞留。
- staleAfterTurns=0 关闭判定；promptHint 形态断言（无滞留=空串）。

## Out of Scope

- 自动清理/降级行为。
- 墙钟时间戳（TodoItem schema 变更）。
- 跨会话聚合面板（dashboard 面后续轮）。

## Further Notes
