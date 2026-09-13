# impl 591 — RateLimitKeyHotspot（effort #837）

## 切片

- `buzhou-resilience/src/main/java/.../resilience/ratelimit/RateLimitKeyHotspot.java` — ConcurrentHashMap<String,Counter>（amount 毫账 AtomicLong）+top 排序。
- `buzhou-resilience/src/test/java/.../resilience/ratelimit/RateLimitKeyHotspotTest.java` — 3 例。

## 口径

- amount 毫账 = ×1000 long 累计（读侧 /1000.0 还原——精度 0.001）。
- totalRequests 在封顶判定前自增（申请意图如实）。

## 验证

mvn -pl buzhou-resilience -am test -Dtest='RateLimitKeyHotspotTest' → 3/3 绿；快照再生 1 新公共类型。
