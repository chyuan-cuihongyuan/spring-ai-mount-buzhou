# impl 596 — EvidenceRefValidity（effort #843）

## 切片

- `buzhou-spill/src/main/java/.../spill/EvidenceRefValidity.java` — 纯静态 audit+TreeSet 典序样本。
- `buzhou-spill/src/test/java/.../spill/EvidenceRefValidityTest.java` — 3 例。

## 口径

- invalidRatio 空集=0（非 1——无引用无断链）。

## 验证

mvn -pl buzhou-spill -am test -Dtest='EvidenceRefValidityTest' → 3/3 绿；快照再生 1 新公共类型。
