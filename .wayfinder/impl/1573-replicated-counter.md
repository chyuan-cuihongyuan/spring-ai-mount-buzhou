# impl 1573 — 复制计数器（spec 2022 / T3145–T3146 / R23）

纵切片：`ReplicatedCounter`（core/concurrent 主）+
`ReplicatedCounterTest`（八用例）。per-writer 分量、merge max、CRDT
三性质。

- 测试：`mvn -pl buzhou-core test -Dtest=ReplicatedCounterTest` 8/8 绿。
