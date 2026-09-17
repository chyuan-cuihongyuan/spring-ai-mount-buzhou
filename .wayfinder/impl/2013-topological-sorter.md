# impl 2013 — Q 会话 R13 拓扑排序器（spec 3012 / T5025–T5026 / R13）

纵切片：TopologicalSorter（core/concurrent）——Kahn + 字典序最小 +
环诚实前缀 + 幂等 sort。

- 验证：`mvn -pl buzhou-core test -Dtest='TopologicalSorterTest'` 全绿。
