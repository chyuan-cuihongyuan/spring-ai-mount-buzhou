# impl 594 — IdleDurationHistogram（effort #841）

## 切片

- `buzhou-core/src/main/java/.../core/session/IdleDurationHistogram.java` — AtomicLongArray 桶+bucketOf 线性扫+humanize/bucketLabel。
- `buzhou-core/src/test/java/.../core/session/IdleDurationHistogramTest.java` — 4 例。

## 口径

- 桶判定：idle < bounds[i] 归 i 桶（左闭右开）。
- longest 用 getAndUpdate max（原子）。

## 验证

mvn -pl buzhou-core -am test -Dtest='IdleDurationHistogramTest' → 4/4 绿；快照再生 1 新公共类型。
