# impl 579 — InjectionParanoiaPolicy（effort #826）

## 切片

- `buzhou-guard/src/main/java/.../guard/classifier/InjectionParanoiaPolicy.java` — Level(带阈值枚举)+OBSERVATION_BAND=0.10+decide 纯静态。
- `buzhou-guard/src/test/java/.../guard/classifier/InjectionParanoiaPolicyTest.java` — 6 例。

## 口径

- 三带区间：BLOCK [threshold,1]、LOG [threshold−0.10, threshold)、ALLOW [0, threshold−0.10)。
- 分数截断先于判定（越界值不产生越界裁决）。

## 验证

mvn -pl buzhou-guard -am test -Dtest='InjectionParanoiaPolicyTest' → 6/6 绿；快照再生 1 新公共类型（嵌套枚举/record 不单列）。
