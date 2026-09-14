---
id: T2333
title: M 系雾区池终态裁定的形状裁决
type: task
status: closed
assignee: zcode-m
blocked-by:
created: 2026-09-15
---

## Question

M 会话第 45 轮（收口前清理）：总图 Not-yet-specified 池的条目终态如何裁定？

## Resolution

**用户常设授权 AFK（可推翻）**

逐条裁定：① MCP 动态危险名单桥——**裁定不做**（McpToolHints 观测面 spec 600 已覆盖可见性 + R8 默认动词/R9 静态桥覆盖拦截面；连接生命周期与静态装配语义错配，边际价值低于复杂度）；② 五-2 全量整改——已裁定（spec 1520 三键边界追认）；③ CounterAtomicitySpreadTest——已由 N 会话承接修复闭环（30197389）；④ 低覆盖清扫——全模块扫描无靶点（K 系清扫后饱和）；⑤ 组合语义缺口（批预算×FAILED_ONLY）——本轮族已闭环（spec 1540）。剩余长期项（DefaultAgentSession 望远镜构造器收拢、DefaultAgentRuntime 拆协作者、双降级存储结构收敛）= major 版重构候选，移交后续会话号段。
