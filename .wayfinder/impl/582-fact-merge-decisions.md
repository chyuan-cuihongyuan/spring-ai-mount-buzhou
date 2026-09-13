# impl 582 — FactMergeDecisionDistribution（effort #829）

## 切片

- `buzhou-memory/src/main/java/.../memory/FactMergeDecisionDistribution.java` — EnumMap<SummarySection, long[3]>+synchronized+快照（SummarySection 在 memory.summary 子包——import 注意）。
- `buzhou-memory/src/test/java/.../memory/FactMergeDecisionDistributionTest.java` — 3 例。

## 口径

- long[3] 槽序=Decision 声明序（CREATED/KEPT/SUPERSEDED）。
- 段行只含 computeIfAbsent 触碰过的段（未发生对账的段不占位）。

## 验证

mvn -pl buzhou-memory -am test -Dtest='FactMergeDecisionDistributionTest' → 3/3 绿；快照再生 1 新公共类型。
