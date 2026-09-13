# 807 — 签名密钥轮换到期审计

> 来源：H 会话第 8 轮 = effort #807 / [T1115](../../.wayfinder/tickets/T1115-key-rotation-audit.md) / [T1116](../../.wayfinder/tickets/T1116-key-rotation-audit-verify.md) / impl 560。
> 借鉴：cert-manager 证书到期监控（≈13K star）。

## Problem

审计签名密钥「用了多久」无人管：SigningKeyRing 是版本制（rotate 原子切换+minVerifyVersion），超龄钥的验证风险（算法退役/泄露窗口）与临期提醒缺位——到期后才发现轮换已晚。

## Solution

`KeyRotationAudit`（guard.audit，纯函数）：

- **三档**：OVERDUE（age ≥ maxAge）/ DUE_SOON（age ≥ maxAge − warnBefore）/ OK。
- **异常面**：UNKNOWN_ACTIVE——active 版本在激活账中不存在（账本落后于环）。
- **排序**：最坏在前（OVERDUE → UNKNOWN_ACTIVE → DUE_SOON → OK，同级 age 降序）。
- **零侵入**：激活时刻由 persister 侧记账提供，SigningKeyRing 不改一行。
- **fail-fast**：maxAge < 1、warnBefore < 0、warnBefore > maxAge、null 账本。

## 兼容性

纯新增静态工具；无配置键（策略由调用方定）。

## 诚实边界

纯读数不轮换；过期钥验证拒绝语义归 minVerifyVersion（审计与执行分离）；脏账（null/负时刻）跳过不计。
