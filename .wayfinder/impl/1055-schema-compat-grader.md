# 1055 — MCP 工具入参 schema 破坏性变更分级

**What to build:** McpSchemaCompatGrader 纯函数（grade：四破坏轴+加法演进+fail-closed，嵌套 SchemaCompatVerdict）+ 九测。

**Blocked by:** None — can start immediately.

**Status:** done

- [x] McpSchemaCompatGrader（mcp，Jackson readTree，闭集 CompatClass）
- [x] McpSchemaCompatGraderTest（九测：四破坏轴/加法/fail-closed/典序）
- [x] spec 1402 + README 行 + api-surface.md L 段 + 快照再生

## Done

验证：`mvn -pl buzhou-mcp -am test -Dtest='McpSchemaCompatGraderTest'` 9/9 绿。
