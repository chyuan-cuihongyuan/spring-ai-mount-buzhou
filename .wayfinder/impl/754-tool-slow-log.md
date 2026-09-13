# 754 — 工具慢调用榜读面

**What to build:** ToolSlowLog（严格大于阈值入有界 FIFO 环 + entries() 新→旧快照 + configureThreshold/reset）+ HookedToolCallback 同点接线 + 边界与有界测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] ToolSlowLog + 嵌套 Entry（toolName/durationMillis/epochMillis/failed）
- [x] 严格大于阈值边界（Redis 口径）
- [x] HookedToolCallback 记录点接线（timer 同点）
- [x] ToolSlowLogTest（阈值边界/FIFO 有界/快照不可变/reset）
- [x] spec 1001 + README 行 + API 快照与 api-surface.md 增行

## Done

验证：`mvn -pl buzhou-core test -Dtest=ToolSlowLogTest` 全绿。commit 见本轮 `feat(core)` 提交。
