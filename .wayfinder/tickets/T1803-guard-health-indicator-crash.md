---
id: T1803
title: guard 健康 indicator 未随条件装配——禁用场景启动崩溃
type: task
status: closed
assignee: zcode-k
blocked-by:
created: 2026-09-14
---

## Question

T1801 R1 补测期间，BuzhouGuardHealthAutoConfigurationTest 显形：单独/禁用场景加载该装配时上下文启动失败（UnsatisfiedDependencyException）。根因与修复形态？

## Resolution

**用户常设授权 AFK（可推翻）**

根因（测试显形，surefire 报告实证）：`GuardHealthIndicatorConfiguration` 内部类仅有 `@ConditionalOnClass(HealthIndicator)` 守卫，其 `@Bean auditChainHealthIndicator(AuditChainHealth delegate)` 无条件要求 `AuditChainHealth`；而外层 `auditChainHealth` 是 `@ConditionalOnBean({AuditRecordStore, SigningKeyRing})` 条件装配。无 store/keyring 时（`buzhou.guard.enabled=false` 关掉整个主装配，或 `buzhou.guard.audit.enabled=false` 关掉签名密钥）→ `AuditChainHealth` 缺席 → indicator 装配 NoSuchBean → **应用启动崩溃**。与该类 Javadoc 设计承诺「独立于模块开关——禁用报 UNKNOWN」相悖：禁用恰恰是崩溃路径。

修复（最小一行）：内部类补 `@ConditionalOnBean({AuditRecordStore.class, SigningKeyRing.class})`——与外层 `auditChainHealth` 同条件，delegates 齐备才装配两个 indicator。语义取舍：无 store/keyring 时 actuator `/health` 不再出现 guard 项（此前是崩溃），UNKNOWN 可见性由 core 的 `BuzhouHealthEndpoint` 承接，不丢失。
