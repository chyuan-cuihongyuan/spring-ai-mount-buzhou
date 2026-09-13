---
id: T1293
title: TurnDeadline 软截止窗口读法的形态裁决
type: task
status: closed
assignee: zcode-i
blocked-by:
created: 2026-09-14
---

## Question

I 会话第 22 轮：TurnDeadline（spec 13 §core-2）只有硬到期语义——「即将到期」的软截止窗口读法（K8s graceful period 的 SIGTERM 预警窗语义）是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（I 会话第 22 轮 = effort #921 / spec 921 / impl 674）：缺口成立——等待点只有「到点硬切」，无「临到期预警」读法（宿主想在预算耗尽前 N 秒开始收尾无从判断）。落点 `TurnDeadline` 值对象层（**不动 exec 内核等待点**——那是后续 hook 集成轮）：① `withinSoftWindow(Duration softWindow)`：非哨兵且 remaining ∈ [0, softWindow]（已到期 false——那属硬截止语义不属预警窗）；② `softDeadlineAt(Duration softWindow)`：软截止绝对时刻 Optional&lt;Instant&gt;（deadline − softWindow；哨兵 empty）。校验 softWindow 非 null 非负。纯值对象增量，既有 remaining/min/at/in 零变化。
