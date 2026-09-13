# 784 — 审计收集器采集与持久化失败计数读面

**What to build:** AuditTrailCollector collected/persistFailures/openSessions 三计数 + 嵌套 AuditIngestStats + stats() + 复用既有骨架测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] collected/persistFailures 计数埋点
- [x] AuditIngestStats 嵌套 record（含 openSessions）+ stats()
- [x] AuditIngestStatsTest（采集/非审计不计/持久化失败/收尾清零/fresh 零值）
- [x] spec 1031 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-guard test -Dtest='AuditIngestStatsTest,AuditTrailCollectorTest'` 全绿。commit 见本轮 `feat(guard)` 提交。
