# impl 2137 — R 会话 R37 标量卡尔曼滤波（spec 4036 / T6073–T6074 / R37）

纵切片：ScalarKalmanFilter（core/metrics）——预测/更新 +
增益闭环 + 读数面 + fail-fast。

- 验证：`mvn -pl buzhou-core test -Dtest='ScalarKalmanFilterTest'` 全绿。
