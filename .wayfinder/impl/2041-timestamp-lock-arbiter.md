# impl 2040 — Q 会话 R41 时间戳锁仲裁器（spec 3040 / T5081–T5082 / R41）

纵切片：TimestampLockArbiter（core/concurrent）——wait-die/
wound-wait 双模式 + 年长序 + 受害者指认。

- 验证：`mvn -pl buzhou-core test -Dtest='TimestampLockArbiterTest'` 全绿。
