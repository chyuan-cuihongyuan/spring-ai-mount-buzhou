---
id: T3185
title: 刻度轮定时器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

海量定时任务的 O(1) 调度怎么纯逻辑化？（spec 2042 / effort #2042 / R43）

## Resolution

**Netty hashed wheel timer 纯逻辑版 `TickWheelTimer`（core/exec）**：
schedule 散槽（W 2 的幂位与取模）+rounds=(delay−1)/W 圈数（首轮访问
即到期者 0）+advance 推进一槽查当前槽（圈数尽到期、未尽 −1 留槽且
无条件写回）+幂等重调度+cancel——O(1) 摊销、tick 由调用方驱动确定性
可回放、无线程。
