# 773 — 事实采集隔离硬化与计数读面

**What to build:** FactCollectorHook 逐定义 try/catch 隔离 + saved/failures 计数 + 嵌套 FactCollectionStats + stats() + 隔离与计数测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] judge/save 双路隔离（其余定义照常、链继续）
- [x] FactCollectionStats 嵌套 record + stats()
- [x] FactCollectionStatsTest（命中/隔离/保存失败/空判定）
- [x] spec 1020 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='FactCollectionStatsTest,GuardFactLoopEndToEndTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
