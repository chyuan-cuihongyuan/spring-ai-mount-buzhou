# impl 2035 — Q 会话 R35 Croston 间歇需求预测器（spec 3034 / T5069–T5070 / R35）

纵切片：CrostonForecaster（core/metrics）——双分量分开平滑 + 率
口径 + 稀疏度对账。

- 验证：`mvn -pl buzhou-core test -Dtest='CrostonForecasterTest'` 全绿。
