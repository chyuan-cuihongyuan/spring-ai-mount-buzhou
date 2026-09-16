# impl 1564 — 启动豁免窗追踪（spec 2013 / T3127–T3128 / R14）

纵切片：`StartupGraceTracker`（core/concurrent 主）+
`StartupGraceTrackerTest`（七用例）。锚定/分流/毕业/重锚/独立语义。

- 测试：`mvn -pl buzhou-core test -Dtest=StartupGraceTrackerTest` 7/7 绿。
