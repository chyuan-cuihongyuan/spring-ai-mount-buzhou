# 1088 — 工具 schema 健康审计

**What to build:** ToolSchemaHealthAudit 纯函数（四态分桶同口径校验器跳过条件+findings 封顶+bypassRatio）+ 五测。

**Blocked by:** None.

**Status:** done

- [x] ToolSchemaHealthAudit（core/exec，private 构造静态面）
- [x] ToolSchemaHealthAuditTest 五测
- [x] spec 1435 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-core -am test -Dtest='ToolSchemaHealthAuditTest'` 5/5 绿。
