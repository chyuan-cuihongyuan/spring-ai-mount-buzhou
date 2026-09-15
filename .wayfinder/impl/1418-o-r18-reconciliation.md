# impl 1418 — O 系 R18 对账轮（R18 = effort #1817 / spec 1817 / T2835-T2836）

**What**：对账轮零生产代码。快照补登前置（R13–R17 五类型）+ 全仓 clean
verify + 台账核账 + Wave 4 排程落图。

**Why**：每 6 轮 SRE PRR 核账（R6/R12 口径第三例行）；快照补登前置已从
教训固化为标准步骤。

**Verify**：clean verify BUILD SUCCESS + OSession1800LedgerAuditTest 四断言绿。

**Status**：done（2026-09-16）
