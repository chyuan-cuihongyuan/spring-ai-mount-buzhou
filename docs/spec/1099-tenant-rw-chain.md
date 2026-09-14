# 1099 — 租户沙箱×读写链路组合测试轮

> 来源：J 会话第 99 轮 = effort #1099（[T1657](../../.wayfinder/tickets/T1657-tenant-rw-shape.md) / [T1658](../../.wayfinder/tickets/T1658-tenant-rw-verify.md) / impl 851）。纯测试轮第十弹（R81–R98 先例）。

## Problem Statement

R45 FileSandbox.forTenant（租户隔离）与 R46/R47 读写工具组合——**租户面读写链路的计数一致性与越界拒绝**无组合验证。

## 目标

新增 `TenantRwChainTest`（buzhou-tools）：forTenant 租户沙箱上写→读链路 + 计数对称恒等 + 越界拒绝计数。零生产改动。

## 兼容性

纯测试增量；无生产代码变更。

## Out of Scope

- 多租户并发隔离压测（另轴）。
