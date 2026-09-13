# impl 598 — RollbackUsageStats（effort #845）

## 切片

- `buzhou-core/src/main/java/.../core/prompt/RollbackUsageStats.java` — LinkedHashMap<Counter>+synchronized+快照（嵌套 Report 补定义）。
- `buzhou-core/src/test/java/.../core/prompt/RollbackUsageStatsTest.java` — 3 例。

## 口径

- lastFrom/lastTo 取最近一次（非历史列表——画像口径）。

## 验证

mvn -pl buzhou-core -am test -Dtest='RollbackUsageStatsTest' → 3/3 绿；快照再生 1 新公共类型。
