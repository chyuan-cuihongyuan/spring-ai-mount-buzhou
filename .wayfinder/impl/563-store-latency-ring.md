# impl 563 — StoreLatencyRing + TimedMessageStore（effort #810）

## 切片

- `buzhou-core/src/main/java/.../core/spi/StoreLatencyRing.java` — per-op Ring（long[] 环+total/max）+nearestRank+封顶 16 操作名（synchronized rings 保护建环竞态）。
- `buzhou-core/src/main/java/.../core/spi/TimedMessageStore.java` — MessageStore 装饰器三方法 finally 计时。
- `buzhou-core/src/test/java/.../core/spi/StoreLatencyRingTest.java` — 7 例。

## 口径

- record 路径：先 rings.get 无锁快路径+建环慢路径 synchronized（双检）。
- ringFull = size ≥ 容量（挤老态标记）。

## 验证

mvn -pl buzhou-core -am test -Dtest='StoreLatencyRingTest' → 7/7 绿；快照再生 2 新公共类型。
