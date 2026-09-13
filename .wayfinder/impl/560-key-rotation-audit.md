# impl 560 — KeyRotationAudit（effort #807）

## 切片

- `buzhou-guard/src/main/java/.../guard/audit/KeyRotationAudit.java` — 静态 audit + rank 排序；Finding/Report record。
- `buzhou-guard/src/test/java/.../guard/audit/KeyRotationAuditTest.java` — 6 例。

## 口径

- DUE_SOON 判据 age ≥ maxAge − warnBefore（临期窗=warnBefore）。
- UNKNOWN_ACTIVE 仅 hasSigningKey 且 activeVersion>0 且账中无该版本时出现。

## 验证

mvn -pl buzhou-guard -am test -Dtest='KeyRotationAuditTest' → 6/6 绿；快照再生 1 新公共类型。
