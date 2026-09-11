---
id: T915
title: 排水 cause E2E 验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T914
created: 2026-09-12
---

## Question

E2E 语义如何钉住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（ShutdownDrainCauseEndToEndTest 3 连跑稳定绿 + core 全模块 1783/1783 零回归）：

- 在途会话（挂死模型）+ shutdownGracefully → session.cancelled 事件 {AFTER_CURRENT_TURN, SHUTDOWN_DRAIN}。
