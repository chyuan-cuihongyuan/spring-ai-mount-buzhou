# impl 2111 — R 会话 R11 UUIDv7 时间有序生成器（spec 4010 / T6021–T6022 / R11）

纵切片：UuidV7Monotonic（core/concurrent）——48 位毫秒 + 12 位单调
计数器 + 借位伪时序。

- 验证：`mvn -pl buzhou-core test -Dtest='UuidV7MonotonicTest'` 全绿。
