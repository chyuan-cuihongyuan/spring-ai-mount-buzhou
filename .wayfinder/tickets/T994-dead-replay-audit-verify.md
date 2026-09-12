---
id: T994
title: 死信重放审计事件的验证
type: task
status: closed
assignee: zcode-g
blocked-by: T993
created: 2026-09-13
---

## Question

重放 N 条 → 指标 delta=N、replayCount/replayedCount 正确？零重放零事件？空转语义不变？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 22 轮）：① 造 3 条死信（4xx FATAL）→ replay → CapturingMetrics 收 dead-replayed、replayCount=1、replayedCount=3、返回 3；② 无死信 replay → 返回 0、零指标事件、replayCount=0；③ 既有死信/重放用例零回归。`mvn -pl buzhou-core -am test` 全绿。
