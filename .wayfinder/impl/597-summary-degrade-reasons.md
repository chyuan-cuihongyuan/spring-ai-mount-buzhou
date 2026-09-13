# impl 597 — SummaryDegradeReasons（effort #844）

## 切片

- `buzhou-memory/src/main/java/.../memory/SummaryDegradeReasons.java` — EnumMap 五态+synchronized+占比降序。
- `buzhou-memory/src/test/java/.../memory/SummaryDegradeReasonsTest.java` — 2 例。

## 口径

- 平局保持枚举声明序（UNKNOWN 殿后兜底语义）。

## 验证

mvn -pl buzhou-memory -am test -Dtest='SummaryDegradeReasonsTest' → 2/2 绿；快照再生 1 新公共类型。
