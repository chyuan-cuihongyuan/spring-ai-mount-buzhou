# impl 1406 — O 系 R6 对账轮（R6 = effort #1805 / spec 1805 / T2811-T2812）

**What**：对账轮零生产代码。全仓 `mvn clean verify`（三门核账）+ api-surface
快照 regenerate（SpillPressureStall/TurnDeadlineBudget/MemoryPromotionAudit/
PrefixBlockHitStats 四类型入账）+ api-surface.md O 系小节 + 并行吸收（J 系
skills RankerDistStats 断链三连环修复）。

**Why**：每 6 轮 SRE PRR 口径核账（J/K 会话先例）——三门绿才准带入 Wave 2；
并行会话共享检出的实时对撞（005cc8f0×a8b7885d 镜像断链）就地裁决，R67/R118
病理的活体一次入档。

**Verify**：`mvn clean verify` BUILD SUCCESS + OSession1800LedgerAuditTest 四断言
绿 + ApiSurfaceSnapshotTest 快照一致。

**Status**：done（2026-09-16）
