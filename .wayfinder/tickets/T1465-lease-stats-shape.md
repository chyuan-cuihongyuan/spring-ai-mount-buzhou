---
id: T1465
title: 凭证租约生命周期计数读面的形态裁决
type: task
status: closed
assignee: zcode-j
blocked-by:
created: 2026-09-14
---

## Question

J 会话第 8 轮：凭证租约生命周期计数读面补全（Vault lease lifecycle）在本仓是否有缺口？形态如何裁决？

## Resolution

**用户常设授权 AFK（可推翻）**

决策（J 会话第 8 轮 = effort #1007 / spec 1007 / impl 760）：缺口成立——SecretLeases 有 issued/expired/revoked 三计数出口，但 **renew 轴完全不可见**：续租成功与续租被拒（租约缺失/已过期满拒——防旧凭证无限复活）都无计数。续租拒绝率高 = TTL 配置过短的运维信号（Vault lease lifecycle 思想）。落点 core.exec：SecretLeases 增 `renewed`/`renewRejected` 两计数（AtomicLong 同款）+ 新公共 record `SecretLeaseStats(issued, expired, revoked, renewed, renewRejected)` + `stats()` 统一快照（既有三 getter 兼容保留）。
