# impl 1572 — 就绪等待门（spec 2021 / T3143–T3144 / R22）

纵切片：`WaitForReadyGate`（core/concurrent 主）+ `WaitForReadyGateTest`
（八用例）。四态询问、预算排队、批量排空、纪律对账。

- 测试：`mvn -pl buzhou-core test -Dtest=WaitForReadyGateTest` 8/8 绿。
