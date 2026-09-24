# impl 2196 — S 会话 S46 Robin Hood Hash Table 劫富济贫哈希表（spec 5045 / T6191–T6192 / S46）

纵切片：RobinHoodHashTable<K,V>（core/metrics）——距离换位
插入 + 后向搬移删除 + maxProbeDistance 均衡读数。

- 验证：`mvn -pl buzhou-core test -Dtest='RobinHoodHashTableTest'` 全绿（MVN_EXIT=0）。
