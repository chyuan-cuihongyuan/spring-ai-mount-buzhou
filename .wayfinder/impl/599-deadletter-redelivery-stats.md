# impl 599 — DeadLetterRedeliveryStats（effort #846）

## 切片

- `buzhou-core/src/main/java/.../core/webhook/DeadLetterRedeliveryStats.java` — AtomicLong 三计数。
- `buzhou-core/src/test/java/.../core/webhook/DeadLetterRedeliveryStatsTest.java` — 3 例。

## 口径

- streak 成功 set(0)、失败 increment——标准连续语义。

## 验证

mvn -pl buzhou-core -am test -Dtest='DeadLetterRedeliveryStatsTest' → 3/3 绿；快照再生 1 新公共类型。
