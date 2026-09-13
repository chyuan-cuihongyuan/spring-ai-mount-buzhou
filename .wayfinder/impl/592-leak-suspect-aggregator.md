# impl 592 — LeakSuspectAggregator（effort #839）

## 切片

- `buzhou-core/src/main/java/.../core/leak/LeakSuspectAggregator.java` — 实现 LeakListener；LinkedHashMap<long[3]>+per-agg synchronized+快照。
- `buzhou-core/src/test/java/.../core/leak/LeakSuspectAggregatorTest.java` — 4 例。

## 口径

- lastSeen 用 System.currentTimeMillis（报告无时刻字段—— wall clock 口径）。
- 稳键=描述前 64 字符（动态后缀分裂缓解）。

## 验证

mvn -pl buzhou-core -am test -Dtest='LeakSuspectAggregatorTest' → 4/4 绿；快照再生 1 新公共类型。
