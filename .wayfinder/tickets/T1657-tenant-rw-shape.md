---
id: T1657
title: 租户沙箱×读写链路组合测试轮的形状裁决
type: task
status: closed
assignee: zcode-j
blocked-by: T1643
created: 2026-09-15
---

## Question

J 会话第 99 轮：租户沙箱组合的增量选什么形状？

## Resolution

**用户常设授权 AFK（可推翻）**

选题：R45 FileSandbox.forTenant（租户隔离沙箱）与 R46/R47 读写工具组合——**租户面严格窄于宿主面下读写链路的计数一致性**无验证。纯测试轮第十弹。

形状裁决：新增 `TenantRwChainTest`（buzhou-tools）——forTenant 租户沙箱上写→读链路 + 计数对称恒等 + 越界拒绝计数。零生产改动。
