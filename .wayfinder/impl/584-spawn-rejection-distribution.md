# impl 584 — SpawnRejectionDistribution（effort #831）

## 切片

- `buzhou-core/src/main/java/.../core/backpressure/SpawnRejectionDistribution.java` — LinkedHashMap<long[2]>+synchronized+快照排序。
- `buzhou-core/src/test/java/.../core/backpressure/SpawnRejectionDistributionTest.java` — 3 例。

## 口径

- lastSeen 语义=该原因最近一次拒绝时刻（乱序到达取 max）。
- 超封顶新原因不入表不计 total（拒绝意图如实丢弃）。

## 验证

mvn -pl buzhou-core -am test -Dtest='SpawnRejectionDistributionTest' → 3/3 绿；快照再生 1 新公共类型。
