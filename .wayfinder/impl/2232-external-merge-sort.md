# impl 2232 — T 会话 T32 External Merge Sort 外归并排序（spec 6031 / T6263–T6264 / T32）

纵切片：ExternalMergeSort（core/fs）——窗内排序成游程+多路
归并（源码随 T30 对账批预入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='ExternalMergeSortTest'` 全绿；随 T30 verify 三门绿。
