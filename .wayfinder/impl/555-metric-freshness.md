# impl 555 — MetricFreshnessTracker（effort #802）

## 切片

- `buzhou-core/src/main/java/.../core/metrics/MetricFreshnessTracker.java` — BuzhouMetrics 装饰器；ConcurrentHashMap<String,Long> 名字封顶 512 + truncated；touch() 封顶判定（首版 CAS 反转被测试抓获——封顶语义「到顶后新名不再入、已有名刷新不受限」）；audit(now, staleAfter) 年龄降序封顶 64；lastWrites() 只读视图。
- `buzhou-core/src/test/java/.../core/metrics/MetricFreshnessTrackerTest.java` — MutableClock；6 例。

## 口径

- gauge 不追踪：gauge 注册即持续采样，无「写入」事件语义——登记时刻不计。
- 名字级：同名异 tag 一体（series 级基数归 Micrometer）。
- 陈旧判据：`now - lastWrite > staleAfter`（严格大于——恰好等于不算陈旧）。

## 验证

mvn -pl buzhou-core -am test -Dtest='MetricFreshnessTrackerTest' → 6/6 绿；快照再生 1 新公共类型。
