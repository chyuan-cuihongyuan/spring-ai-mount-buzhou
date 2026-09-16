# impl 1576 — 一致性哈希环（spec 2025 / T3151–T3152 / R26）

纵切片：`ConsistentHashRing`（core/policy 主）+ `ConsistentHashRingTest`
（七用例）。虚节点环、顺时针归属、最小迁移、均衡分布。

- 测试：`mvn -pl buzhou-core test -Dtest=ConsistentHashRingTest` 7/7 绿。
