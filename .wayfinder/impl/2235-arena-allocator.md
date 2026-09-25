# impl 2235 — T 会话 T35 Arena Allocator 竞技场分配器（spec 6034 / T6269–T6270 / T35）

纵切片：ArenaAllocator（core/memory）——bump 水位+整池回收
+峰值审计（源码随 T30 对账批预入档）。

- 验证：`mvn -pl buzhou-core test -Dtest='ArenaAllocatorTest'` 全绿；随 T30 verify 三门绿。
