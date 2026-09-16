# impl 1566 — 最小 RTT 滑窗滤波器（spec 2015 / T3131–T3132 / R16）

纵切片：`MinRttTracker`（core/metrics 主）+ `MinRttTrackerTest`
（七用例）。滑窗最小、惰性清除、次小接管、新鲜度。

- 测试：`mvn -pl buzhou-core test -Dtest=MinRttTrackerTest` 7/7 绿。
- 教训入档：比较类检测（新最小）必须先取 before 快照再变更集合——
  变更后取值含自身恒假。
