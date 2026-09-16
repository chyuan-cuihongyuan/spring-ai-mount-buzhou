---
id: T3113
title: LWW 寄存器的形状裁决
type: task
status: closed
assignee: zcode-p
blocked-by: []
created: 2026-09-17
---

## Question

多写者无协调并发的收敛语义怎么定？（spec 2006 / effort #2006 / R7）

## Resolution

**Dynamo/CRDT 线程安全 LWW 寄存器 `LastWriteWinsRegister<T>`
（core/concurrent）**：(ts, writerId) 字典序定胜负 + ts 平局 writerId
字典序确定性仲裁（无随机可回放）+ 完全幂等 + 同 writer 同 ts 异值
first-wins 计冲突 + merge 跨实例收敛 + conflictCount/supersededCount
双对账面（时钟平局/乱序到达）。
