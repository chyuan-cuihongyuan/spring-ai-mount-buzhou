# impl 2226 — T 会话 T26 Striped Lock 条带锁（spec 6025 / T6251–T6252 / T26）

纵切片：StripedLock（core/concurrent）——SplitMix64 混淆条带
映射+withLock 重载（源码随 T24 核账批预入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='StripedLockTest'` 全绿；随 T24 verify 三门绿。
