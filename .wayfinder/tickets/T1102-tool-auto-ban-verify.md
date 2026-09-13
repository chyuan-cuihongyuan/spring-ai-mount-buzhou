---
id: T1102
title: 工具自动封禁验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1101]
created: 2026-09-13
---

## Question

封禁触发/解除/边界如何证明？零行为默认如何守住？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 1 轮 = effort #800）：ToolAutoBanHookTest 8 例——达阈值封禁→到期解除（可拨时钟）/滑窗滑出/成功不重置/会话隔离/未监视+空集+null 零行为/snapshot 三口径+active 过期滤除/键封顶 truncated/参数 fail-fast。buzhou-guard 全模块绿（C 会话排除集）。快照门：1 新公共类型入档（带 regenerate 属性）。
