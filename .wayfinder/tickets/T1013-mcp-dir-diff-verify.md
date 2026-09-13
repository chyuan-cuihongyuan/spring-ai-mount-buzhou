---
id: T1013
title: MCP 工具目录差异报告验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1012]
created: 2026-09-12
---

## Question

差异语义与风险标记如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 7 轮 = effort #706）：①三类变更+危险翻转 risky 标记（readOnly true→false risky；title 变化不 risky）；②SERVER_NEW/SERVER_GONE/IN_SYNC 三态+聚合计数；③null fail-fast。buzhou-mcp 全模块零回归（C 会话排除集）。
