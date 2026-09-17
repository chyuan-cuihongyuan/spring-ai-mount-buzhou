---
id: T5061
title: Q 会话 R31 时间轮的形状裁决
type: task
status: closed
assignee: zcode-q
blocked-by: []
created: 2026-09-18
---

## Question

海量短定时任务怎么 O(1) 入轮批量到期？（spec 3030 / effort #3030 / R31）

## Resolution

**HashedWheelTimers（core/concurrent，免线程纯件）**：hashed
timing wheel——schedule O(1) 落槽（deadline/tick%size），大延迟
多轮滞槽；advanceTo 时针掠槽弹到期（deadline 序+同刻 id 序确定
性）；时间调用方传入；守恒 scheduled==fired+pending；粒度诚实
1 tick；取消/多级轮留白。
