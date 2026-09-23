# impl 2154 — S 会话 S4 有界负载一致哈希（spec 5003 / T6107–T6108 / S4）

纵切片：BoundedLoadRing（core/cache）——稳定哈希 + 容量封顶 +
线性探查回退 + ISE 拒配。

- 验证：`mvn -pl buzhou-core test -Dtest='BoundedLoadRingTest'` 全绿。
