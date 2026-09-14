# 1098 — 导出清单校验统计读面

**What to build:** ExportManifestVerifyStats 静态面（三受踪包装+漏斗五计数+reset）+ 三测。

**Blocked by:** None.

**Status:** done

- [x] ExportManifestVerifyStats（core/session，包装式零侵入）
- [x] ExportManifestVerifyStatsTest 三测
- [x] spec 1439 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ExportManifestVerifyStatsTest'` 3/3 绿。
