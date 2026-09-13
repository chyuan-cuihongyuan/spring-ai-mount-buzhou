# impl 589 — HalfOpenProbeStats（effort #836）

## 切片

- `buzhou-resilience/src/main/java/.../resilience/ratelimit/HalfOpenProbeStats.java` — State（Deque<Boolean> 环+计数+streak）+synchronized per-state。
- `buzhou-resilience/src/test/java/.../resilience/ratelimit/HalfOpenProbeStatsTest.java` — 4 例。

## 口径

- streak 在成功时归零（失败递增）——标准连续计数语义。
- 近窗率=环内真值占比（空环 0）。

## 验证

mvn -pl buzhou-resilience -am test -Dtest='HalfOpenProbeStatsTest' → 4/4 绿；快照再生 1 新公共类型。
