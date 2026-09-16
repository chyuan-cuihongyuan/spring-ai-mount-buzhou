# impl 1579 — 并发组闸（spec 2028 / T3157–T3158 / R29）

纵切片：`ConcurrencyGroupGate`（core/exec 主）+
`ConcurrencyGroupGateTest`（八用例）。互斥、取代、属主栅栏、四计数。

- 测试：`mvn -pl buzhou-core test -Dtest=ConcurrencyGroupGateTest` 8/8 绿。
