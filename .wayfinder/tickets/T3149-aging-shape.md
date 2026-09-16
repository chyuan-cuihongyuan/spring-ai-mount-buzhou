---
id: T3149
title: 老化优先级队列的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

静态优先级的饥饿怎么时间维根治？（spec 2024 / effort #2024 / R25）

## Resolution

**OS aging 线程安全队列 `AgingPriorityQueue<T>`（core/exec）**：有效
优先级 = base + 等待秒×agingRate（默认 1 点/秒——等待生息）+poll 取
有效最大同分 FIFO+零速率退化静态+快照观测面不出队+O(n) 线性扫描诚实
边界——等得够久的低优先级必反超新来的高优先级，饥饿有时间下界。
