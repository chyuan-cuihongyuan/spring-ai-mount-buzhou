# impl 580 — PricingCoverageAudit（effort #827）

## 切片

- `buzhou-core/src/main/java/.../core/budget/PricingCoverageAudit.java` — 纯静态 audit+matches 三层+normalizeAll（键集双录原值+小写）。
- `buzhou-core/src/test/java/.../core/budget/PricingCoverageAuditTest.java` — 5 例。

## 口径

- 空调用 coverageRatio=1.0（无调用无缺口——空真语义与 808 不同，声明在 javadoc）。
- called 去重先于计数（Set seen）。

## 验证

mvn -pl buzhou-core -am test -Dtest='PricingCoverageAuditTest' → 5/5 绿；快照再生 1 新公共类型。
