---
id: T1045
title: MCP 每连接并发占用视图验证
type: task
status: closed
assignee: zcode-g
blocked-by: [T1044]
created: 2026-09-13
---

## Question

占用实时性如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（G 会话第 23 轮 = effort #722）：真 registry+gated 工具——空闲 available=limit、在飞 available=limit−1+inFlight=1、释放回落；接口 default 空视图+UNSET 哨兵。buzhou-mcp 全模块零回归（C 会话排除集）。
