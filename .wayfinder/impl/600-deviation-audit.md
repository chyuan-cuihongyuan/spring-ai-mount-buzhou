# impl 600 续 — ConfigDeviationAudit（effort #848）

## 切片

- `buzhou-core/src/main/java/.../core/config/ConfigDeviationAudit.java` — 纯静态 audit+TreeMap 典序遍历。
- `buzhou-core/src/test/java/.../core/config/ConfigDeviationAuditTest.java` — 3 例。

## 口径

- configured 只计「有基线且当前值非空」的键。
- 偏离=current != default（String.equals）。

## 验证

mvn -pl buzhou-core -am test -Dtest='ConfigDeviationAuditTest' → 3/3 绿；快照再生 1 新公共类型。
