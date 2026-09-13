# impl 590 — LeaderElectionStats（effort #838）

## 切片

- `buzhou-store-redis/src/main/java/.../store/redis/LeaderElectionStats.java` — AtomicLong 四计数+contentionRatio。
- `buzhou-store-redis/src/test/java/.../store/redis/LeaderElectionStatsTest.java` — 3 例。

## 口径

- 烈度分母含全部四态（总尝试口径）。

## 验证

mvn -pl buzhou-store-redis -am test -Dtest='LeaderElectionStatsTest' → 3/3 绿；快照再生 1 新公共类型。
