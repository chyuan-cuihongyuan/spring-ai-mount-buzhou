# impl 595 — AuthDecisionStats（effort #842）

## 切片

- `buzhou-guard/src/main/java/.../guard/hook/AuthDecisionStats.java` — EnumMap 五态+synchronized+占比降序快照。
- `buzhou-guard/src/test/java/.../guard/hook/AuthDecisionStatsTest.java` — 3 例。

## 口径

- 平局保持枚举声明序（EnumMap 迭代序）。

## 验证

mvn -pl buzhou-guard -am test -Dtest='AuthDecisionStatsTest' → 3/3 绿；快照再生 1 新公共类型。
