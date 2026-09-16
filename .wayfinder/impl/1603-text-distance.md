# impl 1603 — 文本编辑距离（spec 2052 / T3205–T3206 / R53）

纵切片：`TextDistance`（core/metrics 主）+ `TextDistanceTest`（八用例）。
两行 DP、归一相似比、阈值近匹配。

- 测试：`mvn -pl buzhou-core test -Dtest=TextDistanceTest` 8/8 绿。
