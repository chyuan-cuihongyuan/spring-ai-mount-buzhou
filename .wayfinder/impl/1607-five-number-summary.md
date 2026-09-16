# impl 1607 — 五数概括与 IQR 围栏（spec 2056 / T3213–T3214 / R57）

纵切片：`FiveNumberSummary`（core/metrics 主）+
`FiveNumberSummaryTest`（八用例）。R-7 分位、Tukey 围栏、稳健离群。

- 测试：`mvn -pl buzhou-core test -Dtest=FiveNumberSummaryTest` 8/8 绿。
