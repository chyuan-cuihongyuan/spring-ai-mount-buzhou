---
id: T919
title: 回放起点 state 验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T918
created: 2026-09-12
---

## Question

起点 state 如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（SessionForkLineageTest 3/3 扩展 + core 全模块 1783/1783 零回归）：forkFromTurn(upToTurn=1) → state["buzhou.fork.turn"]=="1" 且 source 键同在。
