# impl 1578 — EWMA 估计器（spec 2027 / T3155–T3156 / R28）

纵切片：`EwmaEstimator`（core/metrics 主）+ `EwmaEstimatorTest`（八用例）。
锚定、α 递推、两极、收敛、单调、reset。

- 测试：`mvn -pl buzhou-core test -Dtest=EwmaEstimatorTest` 8/8 绿。
- 教训入档：收敛断言容差要按 (1−α)^n 数学口径定，非拍 1e-6。
