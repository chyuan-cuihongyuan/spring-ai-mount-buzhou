---
id: T863
title: 取消原因验证口径
type: task
status: closed
assignee: zcode-f
blocked-by: T862
created: 2026-09-12
---

## Question

cause 传播如何钉住不回归？

## Resolution

**用户常设授权 AFK（可推翻）**

验证口径（CancelCauseTest 3/3，buzhou-core 全模块 1749/1749 零回归——含既有 cancel/停机/黄金轨迹测试）：

- 既有 cancel(Mode)：事件 {cancelMode, cause=USER}。
- 显式 cancel(Mode, DEADLINE)：payload 带 DEADLINE。
- null cause → USER。
- 停机路径 SHUTDOWN_DRAIN 接线由既有 shutdown 测试回归覆盖（签名兼容）。
