# impl 574 — LintSeverityGrader（effort #821）

## 切片

- `buzhou-core/src/main/java/.../core/exec/LintSeverityGrader.java` — Severity(rank)+默认 Map.copyOf+withRule 双构造+grade 排序计数。
- `buzhou-core/src/test/java/.../core/exec/LintSeverityGraderTest.java` — 5 例。

## 口径

- rank 与枚举序一致（DENY=0）——排序按 rank+tool+rule 三级。
- isSameAs 断言锁定脏入参返回原实例（不新建）。

## 验证

mvn -pl buzhou-core -am test -Dtest='LintSeverityGraderTest' → 5/5 绿；快照再生 1 新公共类型（枚举嵌套不单列）。
