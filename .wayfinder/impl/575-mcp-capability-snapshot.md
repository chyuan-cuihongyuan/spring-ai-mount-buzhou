# impl 575 — McpCapabilitySnapshot（effort #822）

## 切片

- `buzhou-mcp/src/main/java/.../mcp/McpCapabilitySnapshot.java` — 纯静态 of+fingerprint（排序归一）。
- `buzhou-mcp/src/test/java/.../mcp/McpCapabilitySnapshotTest.java` — 5 例（伪连接三观察点可控）。

## 口径

- callbacks 提取名前判 cb/getToolDefinition null——脏元素跳过。
- hint 计数遍历 values 时 null hint 跳过。

## 验证

mvn -pl buzhou-mcp -am test -Dtest='McpCapabilitySnapshotTest' → 5/5 绿；快照再生 1 新公共类型（Snapshot 嵌套）。
