# impl 2034 — Q 会话 R34 2Q 双队列缓存（spec 3033 / T5067–T5068 / R34）

纵切片：TwoQueueCache（core/cache）——A1in FIFO + Am LRU + 二触
晋升 + 双逐出对账。

- 验证：`mvn -pl buzhou-core test -Dtest='TwoQueueCacheTest'` 全绿。
