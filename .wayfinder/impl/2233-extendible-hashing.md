# impl 2233 — T 会话 T33 Extendible Hashing 可扩目录哈希（spec 6032 / T6265–T6266 / T33）

纵切片：ExtendibleHashing（core/metrics）——目录翻倍+单桶
分裂（源码随 T30 对账批预入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='ExtendibleHashingTest'` 全绿；随 T30 verify 三门绿。
