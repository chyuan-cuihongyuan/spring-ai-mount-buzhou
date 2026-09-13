# 790 — 摘要桥操作与代数回退读面

**What to build:** SummaryStoreBridge saves/loads/generationRegressions 三计数 + per-session lastGeneration 有界 LRU + 嵌套 SummaryStoreStats + stats() + 回退探测测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] saves/loads/generationRegressions 三计数
- [x] lastGeneration 有界 LRU（1024，TurnTimingHook 同先例）
- [x] SummaryStoreStats 嵌套 record + stats()
- [x] SummaryBridgeStatsTest（计数/回退探测/fresh 零值）
- [x] spec 1038 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory test -Dtest='SummaryBridgeStatsTest,ManualCompactorTest'` 全绿。commit 见本轮 `feat(memory)` 提交。
