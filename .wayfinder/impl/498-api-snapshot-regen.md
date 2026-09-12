# 498 — API 快照再生收口

**What to build:** regenerateSnapshot 合规触发再快照 + api-surface.md 补两行 + 全仓 verify 复验。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 快照再生（diff 恰 3 行：SessionForkKeys / ResponseCacheCoalescer / RollingJsonlWriter）
- [x] api-surface.md 同步（core 主段 + resilience 段）
- [x] reactor 口径快照门绿
- [x] 全仓 mvn verify 绿
- [x] spec 645 + README 行

## Done

验证：全仓 `mvn -B -ntp clean verify` 绿。commit 见本轮 `docs(core)` 提交。
