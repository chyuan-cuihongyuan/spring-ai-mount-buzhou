# impl 1555 — φ 累积故障嫌疑度检测器（spec 2004 / T3109–T3110 / R5）

纵切片：`PhiAccrualFailureDetector`（core/concurrent 主）+
`PhiAccrualFailureDetectorTest`（八用例）。间隔滑动窗正态模型、erf
有理近似（A-S 7.1.26）、φ 钳 [0,12]、std 下界防退化、时间外注入
确定性。

- 测试：`mvn -pl buzhou-core test -Dtest=PhiAccrualFailureDetectorTest` 8/8 绿。
