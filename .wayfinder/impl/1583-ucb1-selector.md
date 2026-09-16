# impl 1583 — UCB1 选择器（spec 2032 / T3165–T3166 / R33）

纵切片：`Ucb1Selector`（core/concurrent 主）+ `Ucb1SelectorTest`
（七用例）。UCB 半径、未试优先、收敛与反饿死。

- 测试：`mvn -pl buzhou-core test -Dtest=Ucb1SelectorTest` 7/7 绿。
