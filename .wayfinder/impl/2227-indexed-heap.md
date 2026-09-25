# impl 2227 — T 会话 T27 Indexed Heap 索引堆（spec 6026 / T6253–T6254 / T27）

纵切片：IndexedHeap（core/concurrent）——位置映射+双向
sift 的 decrease-key 堆（源码随 T24 核账批预入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='IndexedHeapTest'` 全绿；随 T24 verify 三门绿。
