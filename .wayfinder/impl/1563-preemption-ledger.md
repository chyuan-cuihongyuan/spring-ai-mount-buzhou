# impl 1563 — 抢占重算账本（spec 2012 / T3125–T3126 / R13）

纵切片：`PreemptionLedger`（core/exec 主）+ `PreemptionLedgerTest`
（六用例）。双面入账、幂等重算、净收益/浪费率/重算率读数。

- 测试：`mvn -pl buzhou-core test -Dtest=PreemptionLedgerTest` 6/6 绿。
