# impl 2183 — S 会话 S33 B+ Tree 有序索引（spec 5032 / T6165–T6166 / S33）

纵切片：BPlusTree（core/metrics）——高扇出确定性平衡树 +
叶链顺序扫 + 对半分裂（复制/移动上提分面）+ TreeMap 圣像。

- 验证：`mvn -pl buzhou-core test -Dtest='BPlusTreeTest'` 全绿。
