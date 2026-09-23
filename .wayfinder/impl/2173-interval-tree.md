# impl 2173 — S 会话 S23 区间树 stabbing 查询（spec 5022 / T6145–T6146 / S23）

纵切片：IntervalTree（core/metrics）——中点分桶建树 + stabbing
下推 + 结果字典序。

- 验证：`mvn -pl buzhou-core test -Dtest='IntervalTreeTest'` 全绿。
