# 761 — spill 回读命中率读面

**What to build:** OnloadHook attempts/loaded/failed 计数（守恒）+ SpillOnloadStats record + stats() + 固定骨架确定性测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] OnloadHook 三计数（守恒 attempts == loaded + failed）
- [x] SpillOnloadStats record + stats()
- [x] SpillOnloadStatsTest（成功/失败/空参数不计/守恒混合）
- [x] spec 1008 + README 行 + API 快照增行

## Done

验证：`mvn -pl buzhou-spill test -Dtest='SpillOnloadStatsTest,OnloadHookTest'` 全绿。commit 见本轮 `feat(spill)` 提交。
