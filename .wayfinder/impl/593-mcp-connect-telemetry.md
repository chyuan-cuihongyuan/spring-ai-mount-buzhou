# impl 593 — McpConnectTelemetry（effort #840）

## 切片

- `buzhou-mcp/src/main/java/.../mcp/McpConnectTelemetry.java` — synchronized servers 建态+per-state synchronized+近窗 boolean 环。
- `buzhou-mcp/src/test/java/.../mcp/McpConnectTelemetryTest.java` — 4 例。

## 口径

- lastDuration 负值=未知（保留上次已知）。
- worstFirst 平局按 server 名典序。

## 验证

mvn -pl buzhou-mcp -am test -Dtest='McpConnectTelemetryTest' → 4/4 绿；快照再生 1 新公共类型。
