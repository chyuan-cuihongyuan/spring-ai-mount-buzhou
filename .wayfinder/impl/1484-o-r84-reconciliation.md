# impl 1484 — O 系 R84 对账轮（R84 = effort #1883 / spec 1883 / T2967-T2968）

**What**：对账轮零生产代码。快照补登（五类型 1091→1096）+
api-surface.md 同步 + README 先落 + 全仓离线 verify + 台账核账 +
Wave 15 排程落图。

**Why**：每 6 轮 SRE PRR 核账第十四例行；撞坑三连（R79/R81/R82
选题被并行会话占坑）入档——选题池换血完成。

**Verify**：全仓 verify BUILD SUCCESS（非 clean 离线口径，偏离已
入档）+ OSession1800LedgerAuditTest 四断言绿。

**Status**：done（2026-09-23）
