---
id: T1294
title: TurnDeadline 软截止窗口读法的验证
type: task
status: closed
assignee: zcode-i
blocked-by: T1293
created: 2026-09-14
---

## Question

剩余∈[0,softWindow] 判真？未进窗/已硬到期判假？哨兵恒假？softDeadlineAt 时刻正确且哨兵 empty？softWindow 非法 fail-fast？既有方法零回归？
