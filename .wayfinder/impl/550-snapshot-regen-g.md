# 550 — API 快照再生 + api-surface.md 同步

**What to build:** 全量 reactor regenerate（8 新公共类入档）+ api-surface.md 同步 8 行。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] regenerateSnapshot（全量 reactor，diff 恰 8 行零意外）
- [x] api-surface.md 同步
- [x] spec 748 + README 行
- [x] 全仓 verify 绿

## Done

验证：全仓 `mvn -B -ntp clean verify` 绿。commit 见本轮 `docs(core)` 提交。
