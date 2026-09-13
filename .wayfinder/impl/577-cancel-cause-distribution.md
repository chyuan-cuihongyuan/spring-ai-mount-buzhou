# impl 577 — CancelCauseDistribution（effort #824）

## 切片

- `buzhou-core/src/main/java/.../core/session/CancelCauseDistribution.java` — EnumMap 双表（计数+lastSeen）+synchronized 记账+快照排序。
- `buzhou-core/src/test/java/.../core/session/CancelCauseDistributionTest.java` — 3 例。

## 口径

- dominant 判定 `count > best`（严格大于）——平局保持先迭代者（声明序）。
- lastSeen merge Math::max（乱序到达取最大）。

## 验证

mvn -pl buzhou-core -am test -Dtest='CancelCauseDistributionTest' → 3/3 绿；快照再生 1 新公共类型。
