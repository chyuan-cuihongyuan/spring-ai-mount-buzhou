# impl 1588 — 同步副本追踪器（spec 2037 / T3175–T3176 / R38）

纵切片：`InSyncTracker`（core/recovery 主）+ `InSyncTrackerTest`
（七用例）。追上时刻锚定、剔除回归、收缩计数。

- 测试：`mvn -pl buzhou-core test -Dtest=InSyncTrackerTest` 7/7 绿。
