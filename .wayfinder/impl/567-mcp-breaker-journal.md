# impl 567 — McpBreakerTransitionJournal（effort #814）

## 切片

- `buzhou-mcp/src/main/java/.../mcp/breaker/McpBreakerTransitionJournal.java` — 环+聚合+dropped+snapshot（702 同构）。
- `buzhou-mcp/src/main/java/.../mcp/breaker/McpServerBreaker.java` — 补丁：journal 字段+2 参构造+lastStates+三路径 journalIfChanged（stateOf 差分）。
- `buzhou-mcp/src/test/java/.../mcp/breaker/McpBreakerTransitionJournalTest.java` — 4 例（本地 FlakyTool stub——504 的为测试内私有类）。

## 口径

- lastStates 首见初始化不产生假变迁（previous==null 跳过）。
- 聚合封顶后明细仍全记（两账独立——与 809 同口径）。

## 验证

mvn -pl buzhou-mcp -am test -Dtest='McpBreakerTransitionJournalTest,McpServerBreakerTest' → 8/8 绿；快照再生 1 新公共类型。
