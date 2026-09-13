# impl 587 — ContextTruncationStats（effort #834）

## 切片

- `buzhou-core/src/main/java/.../core/spi/ContextTruncationStats.java` — LinkedHashMap<long[2]>+synchronized+溢出桶。
- `buzhou-core/src/test/java/.../core/spi/ContextTruncationStatsTest.java` — 3 例。

## 口径

- 溢出桶按量聚合（chars 净计入 totalChars）——名不可溯但量不丢。
- 键封顶判定在首次入账时（已有键不受影响）。

## 验证

mvn -pl buzhou-core -am test -Dtest='ContextTruncationStatsTest' → 3/3 绿；快照再生 1 新公共类型。
