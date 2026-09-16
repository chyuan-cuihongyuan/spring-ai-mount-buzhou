# impl 1611 — BH-FDR 校正（spec 2060 / T3221–T3222 / R61）

纵切片：`FalseDiscoveryRate`（core/eval 主）+ `FalseDiscoveryRateTest`
（六用例）。截止序、功效对比、与 Holm 配对。

- 测试：`mvn -pl buzhou-core test -Dtest=FalseDiscoveryRateTest` 6/6 绿。
- 教训入档：对照组（Holm）期望值也要手算验证——直觉断言必翻车。
