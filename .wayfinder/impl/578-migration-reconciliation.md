# impl 578 — MigrationReconciliation（effort #825）

## 切片

- `buzhou-core/src/main/java/.../core/session/MigrationReconciliation.java` — 纯静态 verify+turnRange+missingStateKeys+truncated。
- `buzhou-core/src/test/java/.../core/session/MigrationReconciliationTest.java` — 6 例（StateEntry 6 参构造对齐 core.spi）。

## 口径

- keepIds 判定=两导出 sessionId 相等（重映射则不等——跳过 id 边界）。
- turns=范围内轮数（max-min+1；空消息=0）。

## 验证

mvn -pl buzhou-core -am test -Dtest='MigrationReconciliationTest' → 6/6 绿；快照再生 1 新公共类型。
