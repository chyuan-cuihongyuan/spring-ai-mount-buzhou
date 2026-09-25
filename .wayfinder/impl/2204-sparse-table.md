# impl 2204 — T 会话 T4 Sparse Table 稀疏表（spec 6003 / T6207–T6208 / T4）

纵切片：SparseTable（core/metrics）——倍增表 O(n log n) 预
计算 + O(1) 幂等重叠查询 + 暴力扫圣像。

- 验证：`mvn -pl buzhou-core test -Dtest='SparseTableTest'` 全绿（MVN_EXIT=0）。
