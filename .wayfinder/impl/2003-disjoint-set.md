# impl 2003 — Q 会话 R3 并查集（spec 3002 / T5005–T5006 / R3）

纵切片：DisjointSet（core/concurrent）——find 路径减半 + union
按秩挂接 + 冗余合并不动账 + componentCount/sizeOf 读数 + 越界
fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='DisjointSetTest'` 全绿。
