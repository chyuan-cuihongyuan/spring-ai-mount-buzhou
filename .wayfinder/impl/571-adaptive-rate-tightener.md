# impl 571 — AdaptiveRateTightener（effort #818）

## 切片

- `buzhou-resilience/src/main/java/.../resilience/ratelimit/AdaptiveRateTightener.java` — ModelState（multiplier+lastThrottle）+synchronized per-state+Math.pow 步进推导。
- `buzhou-resilience/src/test/java/.../resilience/ratelimit/AdaptiveRateTightenerTest.java` — 6 例。

## 口径

- 恢复步起算：over = now − lastThrottle − tightHold；steps = ⌊over/recoverStep⌋。
- onThrottled 同步更新双字段（单锁原子——无中间态可见）。

## 验证

mvn -pl buzhou-resilience -am test -Dtest='AdaptiveRateTightenerTest' → 6/6 绿；快照再生 1 新公共类型。
