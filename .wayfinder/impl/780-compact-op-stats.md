# 780 — 手动压缩操作分布读面

**What to build:** ManualCompactor 五计数（attempts/completed/skipped/failed/foldedMessages 守恒）+ 嵌套 CompactOpStats + stats() + 复用既有骨架测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 五计数埋点（完成/跳过/失败归因点）
- [x] CompactOpStats 嵌套 record + stats()
- [x] CompactOpStatsTest（完成/幂等跳过/失败/守恒/fresh 零值）
- [x] spec 1027 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory test -Dtest='CompactOpStatsTest,ManualCompactorTest'` 全绿。commit 见本轮 `feat(memory)` 提交。
