# impl 2116 — R 会话 R16 Slab 类装箱（spec 4015 / T6031–T6032 / R16）

纵切片：SlabClassPacker（core/cache）——几何档表 + 最小容纳归档 +
浪费比 + 分配记账。

- 验证：`mvn -pl buzhou-core test -Dtest='SlabClassPackerTest'` 全绿。
