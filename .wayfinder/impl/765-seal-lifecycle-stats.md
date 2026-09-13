# 765 — 加密封存操作生命周期计数读面

**What to build:** EncryptedSessionExport sealed/opened/openRejected 三计数 + SealStats 嵌套 record + stats() + 三拒绝路径测试。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] 三计数（open 三拒绝路径全覆盖，异常照抛）
- [x] SealStats 嵌套 record + stats()
- [x] EncryptedSessionExportStatsTest（seal/open/非封缄/换钥/篡改五路断言）
- [x] spec 1012 + README 行（嵌套类型不动 API 快照）

## Done

验证：`mvn -pl buzhou-core test -Dtest='EncryptedSessionExportStatsTest,EncryptedSessionExportTest'` 全绿。commit 见本轮 `feat(core)` 提交。
