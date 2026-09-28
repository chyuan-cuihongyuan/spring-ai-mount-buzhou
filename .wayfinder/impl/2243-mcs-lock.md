# impl 2243 — T 会话 T43 MCS Lock 队列锁（spec 6043 / T6285–T6286 / T43）

纵切片：McsLock（core/concurrent）——每等待者本地自旋 FIFO
队列锁（源码随 T42 对账批预入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='McsLockTest'` 全绿；随 T42 verify 三门绿。
