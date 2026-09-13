---
id: T1012
title: MCP 工具目录差异报告的裁决
type: task
status: closed
assignee: zcode-g
blocked-by:
created: 2026-09-12
---

## Question

600 有翻转告警但无整目录 plan 式差异报告——升级评审要「这次动了哪些工具」。做 diff 原语吗？危险方向怎么标？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（G 会话第 7 轮 = effort #706 / spec 706 / impl 606）：`McpDirectoryDiff.diff(baseline,current)` 纯函数——per-server 四态+ToolChange 三类（ADDED/REMOVED/HINT_CHANGED 带逐字段 detail）+risky 方向标记（readOnly true→false、destructive false→true）+聚合计数；server/工具名字典序确定序。对比口径与 600 同边界（title+三 hint）。
