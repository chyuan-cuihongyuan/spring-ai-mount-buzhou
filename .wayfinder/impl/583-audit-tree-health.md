# impl 583 — AuditTreeHealthReadout（effort #830）

## 切片

- `buzhou-guard/src/main/java/.../guard/audit/AuditTreeHealthReadout.java` — 纯静态 analyze（32−lz 位技巧）。
- `buzhou-guard/src/test/java/.../guard/audit/AuditTreeHealthReadoutTest.java` — 3 例。

## 口径

- 深度公式：32−numberOfLeadingZeros(n−1)（n≥1；n=1 → 0）。
- nextPow2 = 1<<depth。

## 验证

mvn -pl buzhou-guard -am test -Dtest='AuditTreeHealthReadoutTest' → 3/3 绿；快照再生 1 新公共类型。
