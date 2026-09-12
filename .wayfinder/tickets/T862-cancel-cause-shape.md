---
id: T862
title: 取消原因枚举的形态与默认口径裁决
type: task
status: closed
assignee: zcode-f
blocked-by:
created: 2026-09-12
---

## Question

gRPC 把取消建模为带 status code 的稳定闭集。本仓 session.cancelled 事件只有 cancelMode（何时停），没有 cause（谁/为什么发起）——观测面无法区分「用户按停」与「停机收割」。形态怎么定？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（F 会话第 7 轮 = effort #600 / spec 606 / impl 459）：

1. `CancelCause` 枚举（core.session）：USER / SHUTDOWN_DRAIN / DEADLINE / LEASE_LOST / RUNAWAY——后三个供对应机制后续接入（当前各走异常路径：TIMEOUT 异常、LeaseLost 中止、runaway Block）。
2. `AgentSession.cancel(CancelMode, CancelCause)` default 方法委托旧路径（既有实现零改动）；DefaultAgentSession 覆写：cause 进 `session.cancelled` payload + 新指标 `buzhou.session.cancelled`（tag: cause，闭集低基数）。
3. 既有 cancel()/cancel(mode) 默认 USER；null cause 防御性缺省 USER。
4. 停机排水（DefaultAgentRuntime.cancelQuietly）显式传 SHUTDOWN_DRAIN——两个既有发起者即接线完毕。
