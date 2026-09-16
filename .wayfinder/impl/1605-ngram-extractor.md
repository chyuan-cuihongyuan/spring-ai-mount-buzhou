# impl 1605 — n-gram 特征提取（spec 2054 / T3209–T3210 / R55）

纵切片：`NgramExtractor`（core/metrics 主）+ `NgramExtractorTest`
（八用例）。双口径滑窗、去重保首现、诚实边界。

- 测试：`mvn -pl buzhou-core test -Dtest=NgramExtractorTest` 8/8 绿。
