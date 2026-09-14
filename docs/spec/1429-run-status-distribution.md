# 1429 — 运行状态分布与滞后审计

> 来源：L 会话第 30 轮 = effort #1429（票 T2159 / T2160 / impl 1082）。借鉴：Temporal workflow stats（状态分布 + 进度滞后——恢复承诺的暴露窗口量化）。

## Problem Statement

`RunRegistry`（proactive 恢复：重启后枚举在途 run 续跑）只有 list(status) 原子查询：巡检报表需要的状态分布（RUNNING 淤积）与 **turn 滞后**（currentTurn − lastCompletedTurn = 崩溃时丢失的未持久化轮数——「续跑点恒为 lastCompletedTurn 之后」承诺的暴露窗口）无读面。

## 目标

- `RunStatusDistribution`（core/recovery，纯函数静态面，private 构造）：
  - `analyze(List<RunStateSnapshot>)` → `record Report(statusHistogram, runningLagMax, worstOffenders)`；
  - 状态直方：RunStatus 全枚举预置计 0（RUNNING/INTERRUPTED/COMPLETED 缺省可见）；
  - `TurnLag(sessionId, turnLag)`：滞后 = currentTurn − lastCompletedTurn（负值钳 0）；`runningLagMax` 仅统计 RUNNING 状态（COMPLETED 零滞后不入维）；
  - `worstOffenders`：滞后降序会话 id 典序、零滞后不入榜、容量 3。
- 纯函数零状态：巡检周期性调用，不触 registry 存储。

## 兼容性

纯函数零 IO；只读不裁决（恢复动作归 RunRecoveryService）。

## Out of Scope

- 快照时间新鲜度（updatedAt 已在快照上）。
- 恢复执行联动（读面不裁决）。
- 多 registry 聚合（单列表口径显式）。
