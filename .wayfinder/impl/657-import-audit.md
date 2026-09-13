# 657 — 导入审计与严格模式

**What to build:** SessionExportAudit（audit 树对比 + fromJsonStrict opt-in 抛 SessionImportException）+ AuditReport record + 测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] SessionExportAudit.audit（未知字段/缺失推荐字段/strictCompatible）
- [x] fromJsonStrict（不过抛 SessionImportException / 过则等价宽松解析）
- [x] SessionExportAuditTest
- [x] spec 904 + README 行

## Done

验证：`mvn -pl buzhou-core test -Dtest=SessionExportAuditTest` 全绿。commit 见本轮 `feat(core)` 提交。
