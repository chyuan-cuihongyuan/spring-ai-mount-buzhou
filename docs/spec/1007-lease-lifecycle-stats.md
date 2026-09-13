# 1007 — 凭证租约生命周期计数读面

> 来源：J 会话第 8 轮 = effort #1007（[T1465](../../.wayfinder/tickets/T1465-lease-stats-shape.md) / [T1466](../../.wayfinder/tickets/T1466-lease-stats-verify.md) / impl 760）。借鉴：Vault [lease lifecycle](https://developer.hashicorp.com/vault/docs/concepts/lease)（renew/renew-renewal 拒绝率是 TTL 配置健康度的标准信号）。

## Problem Statement

SecretLeases（spec 153，Vault dynamic secrets 借鉴）有 issued/expired/revoked 三计数出口，但 **renew 轴完全不可见**：续租成功无计数；续租被拒（租约缺失、或已过期满拒——「防旧凭证无限复活」语义）也无计数。续租拒绝率高 = TTL 配置过短的运维信号，现无读面可察。

## 目标

- `SecretLeases` 增量（core.exec）：`renewed` / `renewRejected` 两计数（AtomicLong 同款；renewed 在续租成功路径累加，renewRejected 在租约缺失与过期两条拒绝路径累加）。
- 新公共 record `SecretLeaseStats(issued, expired, revoked, renewed, renewRejected)`（core.exec，api 面）+ `stats()` 统一快照。
- 既有 `issuedCount()/expiredCount()/revokedCount()` 三 getter 兼容保留（与 stats() 同源恒等）。

## 兼容性

纯增量读面：renew/revoke 语义零变化（仅加计数）；无新配置项。

## Out of Scope

- 按 name 分桶的续租统计（基数纪律——凭证名可能含动态段，进程级总数足够）。
- TTL 配置建议面（消费侧另立）。
