# impl 570 — SloMultiWindowBurn（effort #817）

## 切片

- `buzhou-core/src/main/java/.../core/health/SloMultiWindowBurn.java` — 纯静态 evaluate+Verdict(reason 人话)。
- `buzhou-core/src/test/java/.../core/health/SloMultiWindowBurnTest.java` — 6 例。

## 口径

- reason 四分类：双窗共振/快窗独热/慢窗独热/样本不足/双冷（五态完整覆盖判定空间）。

## 验证

mvn -pl buzhou-core -am test -Dtest='SloMultiWindowBurnTest' → 6/6 绿；快照再生 1 新公共类型。
