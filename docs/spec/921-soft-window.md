# 921 — TurnDeadline 软截止窗口读法

> 来源：I 会话第 22 轮 = effort #921（[T1293](../../.wayfinder/tickets/T1293-soft-window-shape.md) / [T1294](../../.wayfinder/tickets/T1294-soft-window-verify.md) / impl 674）。借鉴：K8s terminationGracePeriod（SIGTERM 预警 → 宽限 → 硬切的分层语义）。

## Problem Statement

`TurnDeadline` 只有硬到期：各等待点到点硬切，宿主/模型侧无「预算即将耗尽，开始收尾」的预警读法。分层截止（软窗预警 → 硬切）是优雅收尾的前提。

## 目标

`TurnDeadline` 值对象层新增（**不动 exec 内核等待点**——那是后续集成轮）：

- `withinSoftWindow(Duration softWindow)`：非哨兵且 `remaining ∈ [0, softWindow]`——已到期（remaining=0）**不算**在软窗内（那是硬截止语义，预警窗只覆盖「还剩一点」的区间）；
- `softDeadlineAt(Duration softWindow)`：`Optional<Instant>` 软截止绝对时刻（deadline − softWindow；哨兵 empty）；
- 校验：softWindow 非 null 非负（违者 IllegalArgumentException）；
- 纯值对象增量：remaining/min/at/in/isNone 零变化。

## 兼容性

纯增量：公共类新增方法，零既有行为变化。
