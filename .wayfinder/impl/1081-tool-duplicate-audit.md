# 1081 — 工具目录重名审计

**What to build:** ToolCatalogDuplicateAudit 纯函数（analyze→Report 重名组名典序）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] ToolCatalogDuplicateAudit（core/exec，private 构造静态面）
- [x] ToolCatalogDuplicateAuditTest 五测
- [x] spec 1428 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ToolCatalogDuplicateAuditTest'` 5/5 绿。
