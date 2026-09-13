# 777 — facts 段导入导出行数读面

**What to build:** FactsExporter factsExported/factsImported/importFailures 三计数 + 嵌套 FactsFlowStats + stats() + 直构测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数埋点（导出行数/导入行数/导入失败照抛）
- [x] FactsFlowStats 嵌套 record + stats()
- [x] FactsFlowStatsTest（导出/空段不计/导入/坏 JSON 照抛）
- [x] spec 1024 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-memory test -Dtest='FactsFlowStatsTest'` 全绿 + 既有 FactsExporter 回归绿。commit 见本轮 `feat(memory)` 提交。
