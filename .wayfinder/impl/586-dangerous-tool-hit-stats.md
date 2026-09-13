# impl 586 — DangerousToolHitStats（effort #833）

## 切片

- `buzhou-guard/src/main/java/.../guard/config/DangerousToolHitStats.java` — ConcurrentHashMap<String,Counter>+AtomicLong+top 排序。
- `buzhou-guard/src/test/java/.../guard/config/DangerousToolHitStatsTest.java` — 3 例。

## 口径

- totalHits 在键封顶判定之前自增（命中意图如实——被并入溢出桶也算命中）。
- top(n≤0) 返回空（防御）。

## 验证

mvn -pl buzhou-guard -am test -Dtest='DangerousToolHitStatsTest' → 3/3 绿；快照再生 1 新公共类型。
