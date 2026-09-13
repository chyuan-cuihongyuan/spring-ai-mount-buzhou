---
id: T1146
title: MCP 能力协商快照验证
type: task
status: closed
assignee: zcode-h
blocked-by: [T1145]
created: 2026-09-13
---

## Question

排序/计数/降级/指纹确定性如何证明？

## Resolution

**用户常设授权 AFK（可推翻）**

验证（H 会话第 23 轮 = effort #822）：McpCapabilitySnapshotTest 5 例——排序名册+双 hint 计数+指纹值/指纹确定性三例/names 空降级 callbacks/全炸空真/null 归一+fail-fast。首跑抓获静态 fingerprint 未排序。
