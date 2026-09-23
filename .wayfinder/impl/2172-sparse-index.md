# impl 2172 — S 会话 S22 Sparse Index 稀疏索引（spec 5021 / T6143–T6144 / S22）

纵切片：SparseIndex（core/metrics）——块首键注册 + 二分定位 +
诚实缺口。

- 验证：`mvn -pl buzhou-core test -Dtest='SparseIndexTest'` 全绿。
