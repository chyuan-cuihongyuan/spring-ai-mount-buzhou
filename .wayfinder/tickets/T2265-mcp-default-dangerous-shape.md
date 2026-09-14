---
id: T2265
title: MCP 危险工具默认动词模式（S1 硬偏差修复）的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 8 轮：design-incompleteness S1（spec 14 §F 默认动词模式为空，HITL 挂接链断裂）如何修复？

## Resolution

**用户常设授权 AFK（可推翻）**

现状实证：spec 14 §F 承诺默认动词模式（delete/drop/write/update/remove/send/exec），BuzhouMcpProperties 缺省空列表 + DefaultMcpClientRegistry 注释自认「空 = 不登记」——评审定级硬偏差（恶意 server 的 delete_* 类工具默认不经登记）。

形状：BuzhouMcpProperties 缺省（null/未配置）→ DEFAULT_DANGEROUS_TOOL_PATTERNS 七动词前缀 glob（spec 14 §F 原文）；显式空列表保留 = 用户显式关闭（逃生门：yml `dangerous-tool-patterns: []` 绑定空 List 非 null，语义成立）；显式集透传。影响面收敛：dangerousToolNames() 当前零执行面消费方（仅 health size 读数变化），HITL 自动挂接（S2）另轮 starter 编排。行为变化即 spec 承诺恢复，docs/design-incompleteness.md S1 入档闭环。
